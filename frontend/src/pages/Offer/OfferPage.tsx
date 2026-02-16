import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import LoginModal from '../../components/LoginModal';
import './OfferPage.css';

type AuthState = {
  authenticated: boolean;
  nickname: string;
  email: string;
  role: string;
};

type OfferItem = {
  offerId: number;
  recruiterEmail: string;
  companyName: string;
  positionTitle: string;
  contactEmail: string | null;
  contactPhone: string | null;
  employmentType: string;
  workType: string;
  message: string;
  status: string;
  salaryMin: number | null;
  createdAt: string;
};

type OfferFormState = {
  companyName: string;
  positionTitle: string;
  contactEmail: string;
  contactPhone: string;
  employmentType: string;
  workType: string;
  desiredSalary: string;
  message: string;
};

type PanelType = 'create' | 'list' | 'manage';

const INITIAL_OFFER_FORM: OfferFormState = {
  companyName: '',
  positionTitle: '',
  contactEmail: '',
  contactPhone: '',
  employmentType: 'FULL_TIME',
  workType: 'ONSITE',
  desiredSalary: '',
  message: '',
};

const STATUS_OPTIONS = [
  { value: 'SUBMITTED', label: '제출됨' },
  { value: 'REVIEWED', label: '검토중' },
  { value: 'QNA', label: '질의응답' },
  { value: 'INTERVIEW', label: '면접진행' },
  { value: 'CLOSED', label: '종료' },
] as const;

const EMPLOYMENT_OPTIONS = [
  { value: 'FULL_TIME', label: '정규직' },
  { value: 'INTERN', label: '인턴' },
  { value: 'CONTRACT', label: '계약직' },
  { value: 'PART_TIME', label: '파트타임' },
] as const;

const WORK_TYPE_OPTIONS = [
  { value: 'ONSITE', label: '출퇴근' },
  { value: 'REMOTE', label: '재택 근무' },
  { value: 'HYBRID', label: '하이브리드' },
] as const;

const toKoreanLabel = (type: 'status' | 'employment' | 'workType', value: string) => {
  const map = type === 'status' ? STATUS_OPTIONS : type === 'employment' ? EMPLOYMENT_OPTIONS : WORK_TYPE_OPTIONS;
  return map.find((item) => item.value === value)?.label ?? value;
};

const formatNumberWithWon = (value: string) => {
  const digits = value.replace(/\D/g, '');
  if (!digits) return '';
  return `${Number(digits).toLocaleString('ko-KR')}원`;
};

const parseSalary = (formattedValue: string): number | null => {
  const digits = formattedValue.replace(/\D/g, '');
  if (!digits) return null;
  return Number(digits);
};

const formatPhoneNumber = (value: string) => {
  const digits = value.replace(/\D/g, '').slice(0, 11);
  if (digits.length < 4) return digits;
  if (digits.length < 8) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
};

interface OfferPageProps {
  initialPanel: PanelType;
}

const OfferPage = ({ initialPanel }: OfferPageProps) => {
  const navigate = useNavigate();
  const [panel, setPanel] = useState<PanelType>(initialPanel);
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [auth, setAuth] = useState<AuthState>({ authenticated: false, nickname: '', email: '', role: 'USER' });
  const [offers, setOffers] = useState<OfferItem[]>([]);
  const [statusDrafts, setStatusDrafts] = useState<Record<string, string>>({});
  const [revealedContacts, setRevealedContacts] = useState<Record<string, boolean>>({});
  const [offerForm, setOfferForm] = useState<OfferFormState>(INITIAL_OFFER_FORM);
  const [isOfferLoading, setIsOfferLoading] = useState(false);
  const [isOfferSubmitting, setIsOfferSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [loadingMessage, setLoadingMessage] = useState('처리 중입니다...');
  const [noticeMessage, setNoticeMessage] = useState('');
  const noticeTimerRef = useRef<number | null>(null);

  const isAdmin = useMemo(() => auth.role === 'ADMIN' || auth.role === 'MANAGER', [auth.role]);

  const showNotice = useCallback((message: string) => {
    setNoticeMessage(message);
    if (noticeTimerRef.current) window.clearTimeout(noticeTimerRef.current);
    noticeTimerRef.current = window.setTimeout(() => setNoticeMessage(''), 2200);
  }, []);

  const refreshAuth = useCallback(async () => {
    try {
      const response = await fetch('/api/auth/me', { method: 'GET', credentials: 'same-origin' });
      if (!response.ok) {
        setAuth({ authenticated: false, nickname: '', email: '', role: 'USER' });
        return;
      }
      const data = await response.json();
      if (data?.authenticated) {
        setAuth({ authenticated: true, nickname: data.nickname || 'User', email: data.email || '', role: data.role || 'USER' });
      } else {
        setAuth({ authenticated: false, nickname: '', email: '', role: 'USER' });
      }
    } catch {
      setAuth({ authenticated: false, nickname: '', email: '', role: 'USER' });
    }
  }, []);

  const fetchOffers = useCallback(async () => {
    if (!auth.authenticated) return;
    setIsOfferLoading(true);
    try {
      const response = await fetch('/api/offers', { method: 'GET', credentials: 'same-origin' });
      if (!response.ok) throw new Error('Offer load failed');
      const data: OfferItem[] = await response.json();
      setOffers(data);
      const drafts: Record<string, string> = {};
      data.forEach((offer) => {
        drafts[String(offer.offerId)] = offer.status;
      });
      setStatusDrafts(drafts);
    } catch {
      showNotice('오퍼 목록 조회에 실패했습니다.');
    } finally {
      setIsOfferLoading(false);
    }
  }, [auth.authenticated, showNotice]);

  useEffect(() => {
    refreshAuth();
  }, [refreshAuth]);

  useEffect(() => {
    setPanel(initialPanel);
  }, [initialPanel]);

  useEffect(() => {
    if (panel === 'list' || panel === 'manage') fetchOffers();
  }, [panel, fetchOffers]);

  useEffect(() => {
    return () => {
      if (noticeTimerRef.current) window.clearTimeout(noticeTimerRef.current);
    };
  }, []);

  const changePanel = (next: PanelType) => {
    if (!auth.authenticated) {
      showNotice('로그인 후 이용 가능합니다.');
      setIsLoginModalOpen(true);
      return;
    }
    if (next === 'manage' && !isAdmin) {
      showNotice('관리자만 접근 가능합니다.');
      return;
    }

    setPanel(next);
    if (next === 'create') navigate('/offers');
    if (next === 'list') navigate('/offers/list');
    if (next === 'manage') navigate('/offers/manage');
  };

  const handleLogout = async () => {
    const shouldLogout = window.confirm('로그아웃하시겠습니까?');
    if (!shouldLogout) return;
    setLoadingMessage('로그아웃 처리 중입니다...');
    setIsLoading(true);
    try {
      const response = await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
      if (!response.ok) throw new Error('logout failed');
      await refreshAuth();
      showNotice('로그아웃 되었습니다.');
      navigate('/');
    } catch {
      showNotice('로그아웃 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleOfferInputChange = (key: keyof OfferFormState, value: string) => {
    setOfferForm((prev) => ({ ...prev, [key]: value }));
  };

  const handleCreateOffer = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!auth.authenticated) {
      showNotice('로그인이 필요합니다.');
      return;
    }

    setIsOfferSubmitting(true);
    setLoadingMessage('오퍼 등록 중입니다...');
    setIsLoading(true);

    const salaryValue = parseSalary(offerForm.desiredSalary);

    try {
      const response = await fetch('/api/offers', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify({
          companyName: offerForm.companyName,
          positionTitle: offerForm.positionTitle,
          contactEmail: offerForm.contactEmail || null,
          contactPhone: offerForm.contactPhone || null,
          employmentType: offerForm.employmentType,
          workType: offerForm.workType,
          message: offerForm.message || null,
          salaryMin: salaryValue,
          salaryMax: salaryValue,
          currency: null,
          salaryUnit: null,
        }),
      });

      if (!response.ok) {
        const errText = await response.text();
        throw new Error(errText || 'Offer create failed');
      }

      setOfferForm(INITIAL_OFFER_FORM);
      showNotice('오퍼가 등록되었습니다.');
      setPanel('list');
      navigate('/offers/list');
      await fetchOffers();
    } catch {
      showNotice('오퍼 등록에 실패했습니다.');
    } finally {
      setIsOfferSubmitting(false);
      setIsLoading(false);
    }
  };

  const handleOfferStatusUpdate = async (offerId: number) => {
    const status = statusDrafts[String(offerId)];
    if (!status) return;

    setLoadingMessage('오퍼 상태 변경 중입니다...');
    setIsLoading(true);
    try {
      const response = await fetch(`/api/offers/${offerId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify({ status }),
      });

      if (!response.ok) {
        const errText = await response.text();
        throw new Error(errText || 'Offer status update failed');
      }

      await fetchOffers();
      showNotice('오퍼 상태가 변경되었습니다.');
    } catch {
      showNotice('오퍼 상태 변경에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const revealContact = (offerId: number) => {
    setRevealedContacts((prev) => ({ ...prev, [String(offerId)]: true }));
  };

  return (
    <div className="offer-page">
      <LoginModal
        isOpen={isLoginModalOpen}
        onClose={() => setIsLoginModalOpen(false)}
        onLoginStart={(message) => {
          setLoadingMessage(message || '로그인 처리 중입니다...');
          setIsLoading(true);
        }}
        onLoginEnd={() => setIsLoading(false)}
        onLoginSuccess={async () => {
          await refreshAuth();
          showNotice('로그인 되었습니다.');
        }}
      />

      {isLoading && (
        <div className="loading-overlay" role="status" aria-live="polite">
          <div className="loading-card">
            <div className="loading-spinner"></div>
            <p>{loadingMessage}</p>
          </div>
        </div>
      )}

      {noticeMessage && <div className="notice-toast">{noticeMessage}</div>}

      <header className="offer-topbar">
        <div className="offer-brand" onClick={() => navigate('/')}>AlphaHows</div>
        <div className="offer-tabs">
          <button className={panel === 'create' ? 'active' : ''} onClick={() => changePanel('create')}>오퍼 등록</button>
          <button className={panel === 'list' ? 'active' : ''} onClick={() => changePanel('list')}>오퍼 목록</button>
          {isAdmin && <button className={panel === 'manage' ? 'active' : ''} onClick={() => changePanel('manage')}>오퍼 관리</button>}
        </div>
        <div className="offer-auth">
          {auth.authenticated ? (
            <>
              <span>{auth.nickname}님</span>
              <button onClick={handleLogout}>로그아웃</button>
            </>
          ) : (
            <button onClick={() => setIsLoginModalOpen(true)}>로그인</button>
          )}
        </div>
      </header>

      <section className="offer-content">
        {panel === 'create' && (
          <form className="offer-form" onSubmit={handleCreateOffer}>
            <h3>오퍼 등록</h3>
            <label><span>회사명</span><input type="text" value={offerForm.companyName} onChange={(e) => handleOfferInputChange('companyName', e.target.value)} required /></label>
            <label><span>포지션</span><input type="text" value={offerForm.positionTitle} onChange={(e) => handleOfferInputChange('positionTitle', e.target.value)} required /></label>
            <label><span>연락 이메일</span><input type="email" value={offerForm.contactEmail} onChange={(e) => handleOfferInputChange('contactEmail', e.target.value)} /></label>
            <label><span>연락 전화번호</span><input type="text" value={offerForm.contactPhone} onChange={(e) => handleOfferInputChange('contactPhone', formatPhoneNumber(e.target.value))} placeholder="010-1234-5678" /></label>
            <label><span>고용 형태</span><select value={offerForm.employmentType} onChange={(e) => handleOfferInputChange('employmentType', e.target.value)}>{EMPLOYMENT_OPTIONS.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}</select></label>
            <label><span>근무 형태</span><select value={offerForm.workType} onChange={(e) => handleOfferInputChange('workType', e.target.value)}>{WORK_TYPE_OPTIONS.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}</select></label>
            <label><span>희망 연봉</span><input type="text" value={offerForm.desiredSalary} onChange={(e) => handleOfferInputChange('desiredSalary', formatNumberWithWon(e.target.value))} placeholder="예: 50,000,000원" /></label>
            <label><span>메시지</span><textarea value={offerForm.message} onChange={(e) => handleOfferInputChange('message', e.target.value)} /></label>
            <button type="submit" disabled={isOfferSubmitting}>등록하기</button>
          </form>
        )}

        {(panel === 'list' || panel === 'manage') && (
          <div className="offer-list-wrap">
            <h3>{panel === 'list' ? '오퍼 목록' : '오퍼 관리'}</h3>
            {isOfferLoading && <p className="empty">목록을 불러오는 중입니다...</p>}
            {!isOfferLoading && offers.length === 0 && <p className="empty">등록된 오퍼가 없습니다.</p>}

            {offers.map((offer) => {
              const isRevealed = !!revealedContacts[String(offer.offerId)];
              return (
                <article key={offer.offerId} className="offer-item">
                  <div className="head"><strong>{offer.companyName}</strong><span>{offer.positionTitle}</span></div>
                  <div className="meta">상태: {toKoreanLabel('status', offer.status)}</div>
                  <div className="meta">고용형태: {toKoreanLabel('employment', offer.employmentType)}</div>
                  <div className="meta">근무형태: {toKoreanLabel('workType', offer.workType)}</div>
                  <div className="meta">희망연봉: {offer.salaryMin == null ? '협의' : `${Number(offer.salaryMin).toLocaleString('ko-KR')}원`}</div>
                  <p>{offer.message || '메시지 없음'}</p>

                  {panel === 'manage' && (
                    <>
                      {isRevealed ? (
                        <div className="contact-box">
                          <div>이메일: {offer.contactEmail || '미입력'}</div>
                          <div>전화번호: {offer.contactPhone || '미입력'}</div>
                        </div>
                      ) : (
                        <button className="secondary" onClick={() => revealContact(offer.offerId)}>연락처 확인</button>
                      )}

                      <div className="status-row">
                        <select value={statusDrafts[String(offer.offerId)] || offer.status} onChange={(e) => setStatusDrafts((prev) => ({ ...prev, [String(offer.offerId)]: e.target.value }))}>
                          {STATUS_OPTIONS.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
                        </select>
                        <button onClick={() => handleOfferStatusUpdate(offer.offerId)}>상태 변경</button>
                      </div>
                    </>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
};

export default OfferPage;

