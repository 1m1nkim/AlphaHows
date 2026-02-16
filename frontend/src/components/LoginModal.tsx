import React, { useState } from 'react';
import './LoginModal.css';

interface LoginModalProps {
    isOpen: boolean;
    onClose: () => void;
    onLoginStart: (message?: string) => void;
    onLoginEnd: () => void;
    onLoginSuccess: () => Promise<void> | void;
}

const LoginModal: React.FC<LoginModalProps> = ({
    isOpen,
    onClose,
    onLoginStart,
    onLoginEnd,
    onLoginSuccess,
}) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        setIsSubmitting(true);
        onLoginStart('로그인 처리 중입니다.');

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'same-origin',
                body: JSON.stringify({ email, password }),
            });

            if (response.ok) {
                await onLoginSuccess();
                onClose();
                return;
            }

            const errData = await response.text();
            setError(errData || '로그인에 실패했습니다.');
        } catch (err) {
            setError('로그인 중 네트워크 오류가 발생했습니다.');
        } finally {
            setIsSubmitting(false);
            onLoginEnd();
        }
    };

    return (
        <div className="login-modal-overlay">
            <div className="login-modal-content">
                <button className="close-btn" onClick={onClose} disabled={isSubmitting}>&times;</button>
                <h2>Login</h2>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Email</label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            disabled={isSubmitting}
                        />
                    </div>
                    <div className="form-group">
                        <label>Password</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            disabled={isSubmitting}
                        />
                    </div>
                    {error && <p className="error-msg">{error}</p>}
                    <button type="submit" className="login-submit-btn" disabled={isSubmitting}>Sign In</button>

                    <div className="divider">OR</div>
                    <a
                        href="/oauth2/authorization/kakao"
                        className="kakao-login-btn"
                        onClick={() => onLoginStart('로그인 중입니다 잠시만 기다려주세요.')}
                    >
                        카카오로 로그인
                    </a>
                </form>
            </div>
        </div>
    );
};

export default LoginModal;
