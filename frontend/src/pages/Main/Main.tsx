import { useCallback, useEffect, useRef, useState } from 'react';
import './Main.css';
import LoginModal from '../../components/LoginModal';

type AuthState = {
    authenticated: boolean;
    nickname: string;
    email: string;
};

const Main = () => {
    const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
    const [auth, setAuth] = useState<AuthState>({
        authenticated: false,
        nickname: '',
        email: '',
    });
    const [isLoading, setIsLoading] = useState(false);
    const [loadingMessage, setLoadingMessage] = useState('처리 중입니다...');
    const [noticeMessage, setNoticeMessage] = useState('');
    const noticeTimerRef = useRef<number | null>(null);

    const showNotice = useCallback((message: string) => {
        setNoticeMessage(message);

        if (noticeTimerRef.current) {
            window.clearTimeout(noticeTimerRef.current);
        }

        noticeTimerRef.current = window.setTimeout(() => {
            setNoticeMessage('');
        }, 2200);
    }, []);

    // 서버 세션 기준 현재 로그인 상태를 동기화한다.
    const refreshAuth = useCallback(async () => {
        try {
            const response = await fetch('/api/auth/me', {
                method: 'GET',
                credentials: 'same-origin',
            });

            if (!response.ok) {
                setAuth({ authenticated: false, nickname: '', email: '' });
                return;
            }

            const data = await response.json();
            if (data?.authenticated) {
                setAuth({
                    authenticated: true,
                    nickname: data.nickname || 'User',
                    email: data.email || '',
                });
                return;
            }

            setAuth({ authenticated: false, nickname: '', email: '' });
        } catch (error) {
            setAuth({ authenticated: false, nickname: '', email: '' });
        }
    }, []);

    useEffect(() => {
        refreshAuth();

        // 카카오 로그인 리다이렉트 복귀나 탭 전환 복귀 시 상태를 다시 확인한다.
        const handleFocus = () => {
            refreshAuth();
        };

        const handleVisibility = () => {
            if (!document.hidden) {
                refreshAuth();
            }
        };

        window.addEventListener('focus', handleFocus);
        document.addEventListener('visibilitychange', handleVisibility);

        return () => {
            window.removeEventListener('focus', handleFocus);
            document.removeEventListener('visibilitychange', handleVisibility);
        };
    }, [refreshAuth]);

    useEffect(() => {
        return () => {
            if (noticeTimerRef.current) {
                window.clearTimeout(noticeTimerRef.current);
            }
        };
    }, []);

    const handleLogout = async () => {
        const shouldLogout = window.confirm('로그아웃하시겠습니까?');
        if (!shouldLogout) {
            return;
        }

        setLoadingMessage('로그아웃 처리 중입니다...');
        setIsLoading(true);

        try {
            const response = await fetch('/api/auth/logout', {
                method: 'POST',
                credentials: 'same-origin',
            });

            if (!response.ok) {
                throw new Error('Logout failed');
            }

            await refreshAuth();
            showNotice('로그아웃 되었습니다.');
        } catch (error) {
            showNotice('로그아웃 중 오류가 발생했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="main-container">
            <LoginModal
                isOpen={isLoginModalOpen}
                onClose={() => setIsLoginModalOpen(false)}
                onLoginStart={(message) => {
                    setLoadingMessage(message || '로그인 처리 중입니다...');
                    setIsLoading(true);
                }}
                onLoginEnd={() => {
                    setIsLoading(false);
                }}
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

            <header className="main-header">
                <div className="logo">AlphaHows</div>
                <nav>
                    <button className="nav-btn">Home</button>
                    <button className="nav-btn">About</button>

                    {auth.authenticated ? (
                        <>
                            <span className="nav-user">{auth.nickname}님</span>
                            <button className="nav-btn primary" onClick={handleLogout} disabled={isLoading}>로그아웃</button>
                        </>
                    ) : (
                        <button className="nav-btn primary" onClick={() => setIsLoginModalOpen(true)} disabled={isLoading}>Login</button>
                    )}
                </nav>
            </header>

            <main className="hero-section">
                <div className="hero-content">
                    <h1 className="hero-title">
                        Simplify Your Workflow<br />
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
                        <button className="cta-btn primary" onClick={() => alert('Start Started Clicked!')}>Get Started</button>
                        <button className="cta-btn secondary">Learn More</button>
                    </div>
                </div>
                <div className="hero-visual">
                    <div className="circle-bg"></div>
                    <div className="glass-card">
                        <h3>System Status</h3>
                        <div className="stat-row">
                            <span>Database</span>
                            <span className="status-ok">Connected</span>
                        </div>
                        <div className="stat-row">
                            <span>Server</span>
                            <span className="status-ok">Online</span>
                        </div>
                        <div className="stat-row">
                            <span>Docker</span>
                            <span className="status-ok">Active</span>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    );
};

export default Main;
