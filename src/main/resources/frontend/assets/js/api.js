const Api = (() => {
    const API_BASE = '/api';

    const getToken = () => localStorage.getItem('ec_token');
    const setToken = (token) => localStorage.setItem('ec_token', token);
    const clearToken = () => localStorage.removeItem('ec_token');

    const request = async (path, options = {}) => {
        const headers = options.headers ? { ...options.headers } : {};
        headers['Content-Type'] = 'application/json';
        const token = getToken();
        if (token) headers['Authorization'] = `Bearer ${token}`;

        const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(data.message || 'Erro inesperado');
        }
        return data;
    };

    const ensureAuth = () => {
        if (!getToken() && location.pathname !== '/') {
            location.href = '/';
        }
    };

    const logout = () => {
        clearToken();
        location.href = '/';
    };

    return {
        login: (username, password) => request('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }).then(res => { setToken(res.token); return res; }),
        userInfo: () => request('/secure/userinfo'),
        userTransactions: () => request('/secure/user/transactions'),
        clanTransactions: () => request('/secure/clan/transactions'),
        kingdomTransactions: () => request('/secure/kingdom/transactions'),
        stats: () => request('/secure/stats'),
        claims: () => request('/secure/claims'),
        kingdomInfo: () => request('/secure/kingdom/info'),
        kingdomMembers: () => request('/secure/kingdom/members'),
        kingdomBank: () => request('/secure/kingdom/bank'),
        kingdomClaims: () => request('/secure/kingdom/claims'),
        kingdomUpkeep: () => request('/secure/kingdom/upkeep'),
        clanInfo: () => request('/secure/clan/info'),
        clanMembers: () => request('/secure/clan/members'),
        clanBank: () => request('/secure/clan/bank'),
        clanClaim: () => request('/secure/clan/claim'),
        shops: () => request('/secure/shops'),
        shopDetails: (id) => request(`/secure/shops/${id}`),
        gemPacks: () => request('/secure/gems/packs'),
        purchaseGems: (packId) => request('/secure/gems/purchase', { method: 'POST', body: JSON.stringify({ packId }) }),
        jobs: () => request('/jobs'),
        currentJob: () => request('/secure/jobs/current'),
        portals: () => request('/secure/portals'),
        ensureAuth,
        logout,
        token: getToken
    };
})();

const UI = (() => {
    const formatCurrency = (value) => `ⓔ ${value.toFixed(2)}`;
    const el = (selector) => document.querySelector(selector);
    const setText = (selector, value) => { const node = el(selector); if (node) node.textContent = value; };
    const showError = (selector, message) => { const node = el(selector); if (node) { node.textContent = message; node.hidden = false; } };
    const hide = (selector) => { const node = el(selector); if (node) node.hidden = true; };
    return { formatCurrency, el, setText, showError, hide };
})();
