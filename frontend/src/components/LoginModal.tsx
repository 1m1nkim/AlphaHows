import React, { useState } from 'react';
import './LoginModal.css';

interface LoginModalProps {
    isOpen: boolean;
    onClose: () => void;
}

const LoginModal: React.FC<LoginModalProps> = ({ isOpen, onClose }) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ email, password }),
            });

            if (response.ok) {
                const data = await response.text();
                alert(data); // "Login Successful: ..."
                onClose();
                // 여기에 로그인 성공 후 상태 업데이트 로직 추가 가능
            } else {
                const errData = await response.text();
                setError(errData || 'Login failed');
            }
        } catch (err) {
            setError('Network error occurring during login.');
        }
    };

    return (
        <div className="login-modal-overlay">
            <div className="login-modal-content">
                <button className="close-btn" onClick={onClose}>&times;</button>
                <h2>Login</h2>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Email</label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                        />
                    </div>
                    <div className="form-group">
                        <label>Password</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                    </div>
                    {error && <p className="error-msg">{error}</p>}
                    <button type="submit" className="login-submit-btn">Sign In</button>

                    <div className="divider">OR</div>
                    <a href="/oauth2/authorization/kakao" className="kakao-login-btn">
                        카카오로 로그인
                    </a>
                </form>
            </div>
        </div>
    );
};

export default LoginModal;
