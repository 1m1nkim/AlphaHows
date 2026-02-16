import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import './Main.css';
import LoginModal from '../../components/LoginModal';

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
  createdAt: string | null;
  read: boolean;
  adminRead: boolean;
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

type OfferPanel = 'create' | 'list' | null;
type ReadFilter = 'ALL' | 'READ' | 'UNREAD';

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

const formatCreatedAt = (value: string | null) => {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 16);
};

const toTopicKey = (email: string) => email.toLowerCase().replace(/[^a-z0-9]/g, '_');

const Main = () => {
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [auth, setAuth] = useState<AuthState>({ authenticated: false, nickname: '', email: '', role: 'USER' });
  const [activePanel, setActivePanel] = useState<OfferPanel>(null);
  const [offers, setOffers] = useState<OfferItem[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [offerForm, setOfferForm] = useState<OfferFormState>(INITIAL_OFFER_FORM);
  const [isOfferLoading, setIsOfferLoading] = useState(false);
  const [isOfferSubmitting, setIsOfferSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [loadingMessage, setLoadingMessage] = useState('처리 중입니다...');
  const [noticeMessage, setNoticeMessage] = useState('');
  const [keywordFilter, setKeywordFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | string>('ALL');
  const [readFilter, setReadFilter] = useState<ReadFilter>('ALL');

  const noticeTimerRef = useRef<number | null>(null);
  const stompClientRef = useRef<Client | null>(null);
  const previousAdminReadByOfferRef = useRef<Record<number, boolean>>({});

  const isAdmin = useMemo(() => auth.role.trim().toUpperCase() === 'ADMIN', [auth.role]);

  const showNotice = useCallback((message: string) => {
    setNoticeMessage(message);
    if (noticeTimerRef.current) window.clearTimeout(noticeTimerRef.current);
    noticeTimerRef.current = window.setTimeout(() => setNoticeMessage(''), 2200);
  }, []);

  const syncAdminReadTransitionNotice = useCallback((list: OfferItem[]) => {
    if (isAdmin) return;

    const prevMap = previousAdminReadByOfferRef.current;
    const hasPrevSnapshot = Object.keys(prevMap).length > 0;
    const nextMap: Record<number, boolean> = {};
    let hasNewlyRead = false;

    for (const offer of list) {
      nextMap[offer.offerId] = offer.adminRead;
      if (hasPrevSnapshot && prevMap[offer.offerId] === false && offer.adminRead === true) {
        hasNewlyRead = true;
      }
    }

    previousAdminReadByOfferRef.current = nextMap;
    if (hasNewlyRead) {
      showNotice('관리자가 오퍼를 확인했습니다.');
    }
  }, [isAdmin, showNotice]);

  const fetchUnreadCount = useCallback(async () => {
    if (!auth.authenticated) {
      setUnreadCount(0);
      return;
    }

    try {
      const response = await fetch('/api/offers/unread-count', { method: 'GET', credentials: 'same-origin' });
      if (!response.ok) return;
      const data: { count: number } = await response.json();
      setUnreadCount(data.count ?? 0);
    } catch {
      // ignore
    }
  }, [auth.authenticated]);

  const refreshAuth = useCallback(async () => {
    try {
      const response = await fetch('/api/auth/me', { method: 'GET', credentials: 'same-origin' });
      if (!response.ok) {
        setAuth({ authenticated: false, nickname: '', email: '', role: 'USER' });
        return;
      }

      const data = await response.json();
      if (data?.authenticated) {
        setAuth({
          authenticated: true,
          nickname: data.nickname || 'User',
          email: data.email || '',
          role: String(data.role || 'USER').trim().toUpperCase(),
        });
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
      const params = new URLSearchParams();
      if (statusFilter !== 'ALL') params.set('status', statusFilter);
      if (readFilter === 'READ') params.set('read', 'true');
      if (readFilter === 'UNREAD') params.set('read', 'false');
      if (keywordFilter.trim()) params.set('keyword', keywordFilter.trim());

      const query = params.toString();
      const response = await fetch(`/api/offers${query ? `?${query}` : ''}`, {
        method: 'GET',
        credentials: 'same-origin',
      });

      if (!response.ok) throw new Error('Offer load failed');
      const data: OfferItem[] = await response.json();
      setOffers(data);
      syncAdminReadTransitionNotice(data);
      await fetchUnreadCount();
    } catch {
      showNotice('오퍼 목록 조회에 실패했습니다.');
    } finally {
      setIsOfferLoading(false);
    }
  }, [auth.authenticated, fetchUnreadCount, keywordFilter, readFilter, showNotice, statusFilter, syncAdminReadTransitionNotice]);

  useEffect(() => {
    void refreshAuth();
  }, [refreshAuth]);

  useEffect(() => {
    if (auth.authenticated) {
      void fetchUnreadCount();
    }
  }, [auth.authenticated, fetchUnreadCount]);

  useEffect(() => {
    if (activePanel === 'list') {
      void fetchOffers();
    }
  }, [activePanel, fetchOffers]);

  useEffect(() => {
    if (!auth.authenticated || isAdmin) {
      previousAdminReadByOfferRef.current = {};
    }
  }, [auth.authenticated, isAdmin]);

  useEffect(() => {
    if (!auth.authenticated || isAdmin) {
      return;
    }

    const timerId = window.setInterval(async () => {
      try {
        const response = await fetch('/api/offers', { method: 'GET', credentials: 'same-origin' });
        if (!response.ok) return;
        const data: OfferItem[] = await response.json();
        syncAdminReadTransitionNotice(data);
        await fetchUnreadCount();
      } catch {
        // ignore
      }
    }, 7000);

    return () => window.clearInterval(timerId);
  }, [auth.authenticated, isAdmin, syncAdminReadTransitionNotice, fetchUnreadCount]);

  useEffect(() => {
    return () => {
      if (noticeTimerRef.current) window.clearTimeout(noticeTimerRef.current);
      if (stompClientRef.current?.active) {
        void stompClientRef.current.deactivate();
      }
    };
  }, []);

  useEffect(() => {
    if (!auth.authenticated) {
      if (stompClientRef.current?.active) {
        void stompClientRef.current.deactivate();
      }
      stompClientRef.current = null;
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 3000,
      onConnect: () => {
        const handleNotification = (frame: { body: string }) => {
          try {
            const payload = JSON.parse(frame.body) as { title?: string; message?: string };
            showNotice(payload.title || payload.message || '새 알림이 도착했습니다.');
          } catch {
            showNotice('새 알림이 도착했습니다.');
          }
          void fetchUnreadCount();
          if (activePanel === 'list') {
            void fetchOffers();
          }
        };

        client.subscribe('/user/queue/notifications', handleNotification);
        if (auth.email) {
          client.subscribe(`/topic/notifications/${toTopicKey(auth.email)}`, handleNotification);
        }
      },
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      if (client.active) {
        void client.deactivate();
      }
    };
  }, [activePanel, auth.authenticated, auth.email, fetchOffers, fetchUnreadCount, showNotice]);

  const openPanel = (panel: OfferPanel) => {
    if (!auth.authenticated) {
      showNotice('로그인 후 이용 가능합니다.');
      setIsLoginModalOpen(true);
      return;
    }

    if (panel === 'create' && isAdmin) {
      showNotice('관리자 계정은 오퍼 등록을 사용할 수 없습니다.');
      return;
    }

    setActivePanel(panel);
  };

  const closePanel = () => setActivePanel(null);

  const handleLogout = async () => {
    const shouldLogout = window.confirm('로그아웃하시겠습니까?');
    if (!shouldLogout) return;

    setLoadingMessage('로그아웃 처리 중입니다...');
    setIsLoading(true);
    try {
      await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
      await refreshAuth();
      setOffers([]);
      setUnreadCount(0);
      setActivePanel(null);
      showNotice('로그아웃 되었습니다.');
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
      setActivePanel(null);
      showNotice('오퍼가 등록되었습니다.');
      await fetchUnreadCount();
    } catch {
      showNotice('오퍼 등록에 실패했습니다.');
    } finally {
      setIsOfferSubmitting(false);
      setIsLoading(false);
    }
  };

  const handleReadToggleByAdmin = async (offer: OfferItem) => {
    if (!isAdmin) return;

    setLoadingMessage(offer.read ? '안읽음으로 변경 중입니다...' : '읽음으로 변경 중입니다...');
    setIsLoading(true);
    try {
      const response = await fetch(`/api/offers/${offer.offerId}/read`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify({ read: !offer.read }),
      });

      if (!response.ok) {
        const errText = await response.text();
        throw new Error(errText || 'Offer read update failed');
      }

      await fetchOffers();
      await fetchUnreadCount();
      showNotice('읽음 상태가 변경되었습니다.');
    } catch {
      showNotice('읽음 상태 변경에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleConfirmByUser = async (offerId: number) => {
    if (isAdmin) return;

    setLoadingMessage('확인 처리 중입니다...');
    setIsLoading(true);
    try {
      const response = await fetch(`/api/offers/${offerId}/confirm`, {
        method: 'PATCH',
        credentials: 'same-origin',
      });

      if (!response.ok) {
        const errText = await response.text();
        throw new Error(errText || 'Offer confirm failed');
      }

      await fetchUnreadCount();
      await fetchOffers();
      showNotice('확인되었습니다.');
    } catch {
      showNotice('확인 처리에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const getDisplayRead = (offer: OfferItem) => (isAdmin ? offer.read : offer.adminRead);
  const offerMenuLabel = unreadCount > 0 ? `오퍼관리(${unreadCount})` : '오퍼관리';

  return (
    <div className="main-container">
      <LoginModal
        isOpen={isLoginModalOpen}
        onClose={() => setIsLoginModalOpen(false)}
        onLoginStart={(message) => {
          setLoadingMessage(message || '로그인 처리 중입니다...');
          setIsLoading(true);
        }}
        onLoginEnd={() => setIsLoading(false)}
        onLoginSuccess={refreshAuth}
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

      <header className="main-header">
        <div className="logo">AlphaHows</div>
        <nav>
          <button className="nav-btn">홈</button>
          <button className="nav-btn">소개</button>
          {!isAdmin && <button className="nav-btn" onClick={() => openPanel('create')}>오퍼 등록</button>}
          <button className="nav-btn" onClick={() => openPanel('list')}>{offerMenuLabel}</button>

          {auth.authenticated ? (
            <>
              <span className="nav-user">{auth.nickname}님 ({auth.role})</span>
              <button className="nav-btn primary" onClick={handleLogout}>로그아웃</button>
            </>
          ) : (
            <button className="nav-btn primary" onClick={() => setIsLoginModalOpen(true)}>로그인</button>
          )}
        </nav>
      </header>

      {activePanel && <div className="drawer-backdrop" onClick={closePanel}></div>}

      <aside className={`offer-drawer ${activePanel ? 'open' : ''}`}>
        {activePanel === 'create' && !isAdmin && (
          <div className="offer-pane">
            <div className="offer-pane-header"><h3>오퍼 등록</h3><button onClick={closePanel}>닫기</button></div>
            <form className="offer-form" onSubmit={handleCreateOffer}>
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
          </div>
        )}

        {activePanel === 'list' && (
          <div className="offer-pane">
            <div className="offer-pane-header"><h3>{offerMenuLabel}</h3><button onClick={closePanel}>닫기</button></div>
            <div className="offer-filter-row">
              <input
                type="text"
                value={keywordFilter}
                onChange={(e) => setKeywordFilter(e.target.value)}
                placeholder="회사명/포지션/메시지 검색"
              />
              <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                <option value="ALL">전체 상태</option>
                {STATUS_OPTIONS.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
              </select>
              <select value={readFilter} onChange={(e) => setReadFilter(e.target.value as ReadFilter)}>
                <option value="ALL">확인 상태 전체</option>
                <option value="UNREAD">미확인</option>
                <option value="READ">확인됨</option>
              </select>
              <button
                className="secondary"
                onClick={() => {
                  setKeywordFilter('');
                  setStatusFilter('ALL');
                  setReadFilter('ALL');
                }}
              >
                초기화
              </button>
            </div>

            <div className="offer-list-wrap">
              {isOfferLoading && <p className="empty">목록을 불러오는 중입니다...</p>}
              {!isOfferLoading && offers.length === 0 && <p className="empty">등록된 오퍼가 없습니다.</p>}

              {offers.map((offer) => {
                const displayRead = getDisplayRead(offer);
                return (
                  <article key={offer.offerId} className={`offer-item ${displayRead ? '' : 'unread'}`}>
                    <div className="head"><strong>{offer.companyName}</strong><span>{offer.positionTitle}</span></div>
                    <div className="read-chip">
                      {isAdmin
                        ? (displayRead ? '읽음' : '안읽음')
                        : (displayRead ? '관리자 확인됨' : '관리자 미확인')}
                    </div>
                    {isAdmin && <div className="meta">요청자: {offer.recruiterEmail}</div>}
                    <div className="meta">상태: {toKoreanLabel('status', offer.status)}</div>
                    <div className="meta">고용형태: {toKoreanLabel('employment', offer.employmentType)}</div>
                    <div className="meta">근무형태: {toKoreanLabel('workType', offer.workType)}</div>
                    <div className="meta">희망연봉: {offer.salaryMin == null ? '협의' : `${Number(offer.salaryMin).toLocaleString('ko-KR')}원`}</div>
                    <div className="meta">등록시간: {formatCreatedAt(offer.createdAt)}</div>
                    <p>{offer.message || '메시지 없음'}</p>

                    {isAdmin && (
                      <div className="status-row">
                        <button onClick={() => handleReadToggleByAdmin(offer)}>{displayRead ? '안읽음으로' : '읽음으로'}</button>
                      </div>
                    )}

                    {!isAdmin && !offer.read && (
                      <div className="status-row">
                        <button className="secondary" onClick={() => handleConfirmByUser(offer.offerId)}>확인</button>
                      </div>
                    )}
                  </article>
                );
              })}
            </div>
          </div>
        )}
      </aside>

      <main className="hero-section">
        <div className="hero-content">
          <h1 className="hero-title">
            Simplify Your Workflow
            <br />
            <span className="highlight">AlphaHows</span>
          </h1>
          <p className="hero-subtitle">
            Experience the next generation of automation.
            Deploy, manage, and scale with ease.
          </p>
          <div className="status-badge">
            <span className="dot"></span>
            deployment_test_success_v1
          </div>
          <div className="cta-group">
            {!isAdmin && <button className="cta-btn primary" onClick={() => openPanel('create')}>오퍼 작성</button>}
            <button className="cta-btn secondary" onClick={() => openPanel('list')}>{offerMenuLabel}</button>
          </div>
        </div>

        <div className="hero-visual">
          <div className="circle-bg"></div>
          <div className="glass-card">
            <h3>System Status</h3>
            <div className="stat-row"><span>Database</span><span className="status-ok">Connected</span></div>
            <div className="stat-row"><span>Server</span><span className="status-ok">Online</span></div>
            <div className="stat-row"><span>RAG</span><span className="status-ok">Ready</span></div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Main;