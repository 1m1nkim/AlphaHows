import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Main from './pages/Main/Main';
import OfferPage from './pages/Offer/OfferPage';
import './App.css';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Main />} />
        <Route path="/offers" element={<OfferPage initialPanel="create" />} />
        <Route path="/offers/list" element={<OfferPage initialPanel="list" />} />
        <Route path="/offers/manage" element={<OfferPage initialPanel="manage" />} />
      </Routes>
    </Router>
  );
}

export default App;
