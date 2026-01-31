import { useState } from 'react';
import './Main.css';
import LoginModal from '../../components/LoginModal';

const Main = () => {
    const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);

    return (
        <div className="main-container">
            <LoginModal isOpen={isLoginModalOpen} onClose={() => setIsLoginModalOpen(false)} />
            <header className="main-header">
                <div className="logo">AlphaHows</div>
                <nav>
                    <button className="nav-btn">Home</button>
                    <button className="nav-btn">About</button>
                    <button className="nav-btn primary" onClick={() => setIsLoginModalOpen(true)}>Login</button>
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
                            <span className="status-ok">Acitve</span>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    );
};

export default Main;
