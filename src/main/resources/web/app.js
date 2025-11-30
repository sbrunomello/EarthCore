(() => {
    const usernameInput = document.getElementById('username');
    const loginButton = document.getElementById('login-btn');
    const profileContainer = document.getElementById('profile');
    const statusBadge = document.getElementById('status');
    const mapButton = document.getElementById('map-fullscreen');
    const mapContainer = document.getElementById('map-container');
    const mapUrl = window.MapConfig?.baseUrl;

    const showStatus = (message, tone = 'warning') => {
        statusBadge.textContent = message;
        statusBadge.className = `pill ${tone}`;
        statusBadge.hidden = false;
    };

    const hideStatus = () => {
        statusBadge.hidden = true;
    };

    const buildListItem = (label, value) => `<p class="muted"><strong>${label}:</strong> ${value ?? '—'}</p>`;

    const normalizeMapUrl = (url) => {
        if (!url) return '';
        return url.endsWith('/') ? url : `${url}/`;
    };

    /**
     * Tenta usar fullscreen do navegador para o BlueMap; se falhar, abre uma nova aba.
     * Mantém a UX consistente mesmo com bloqueios de permissões.
     */
    const handleMapFullscreen = async () => {
        const targetUrl = normalizeMapUrl(mapUrl);

        if (document.fullscreenEnabled && mapContainer?.requestFullscreen) {
            try {
                await mapContainer.requestFullscreen();
                return;
            } catch (error) {
                console.warn('Fallback para nova aba após falha no fullscreen do mapa', error);
            }
        }

        if (targetUrl) {
            window.open(targetUrl, '_blank', 'noopener,noreferrer');
        }
    };

    const renderProfile = (data) => {
        const fragments = [];
        fragments.push(buildListItem('Usuário', data.username));
        fragments.push(buildListItem('UUID', data.uuid));
        fragments.push(buildListItem('Saldo', data.balance.toFixed(2)));

        if (data.clan) {
            fragments.push(`<div class="pill">Clã: ${data.clan.name} [${data.clan.tag}] · ${data.clan.role}</div>`);
            fragments.push(buildListItem('Banco do clã', data.clan.bank.toFixed(2)));
        } else {
            fragments.push('<div class="pill danger">Sem clã</div>');
        }

        if (data.kingdom) {
            fragments.push(`<div class="pill">Reino: ${data.kingdom.name} · ${data.kingdom.role}</div>`);
            fragments.push(buildListItem('Tesouro do reino', data.kingdom.treasury.toFixed(2)));
            fragments.push(buildListItem('Chunks reivindicados', data.kingdom.claims));
        } else {
            fragments.push('<div class="pill danger">Sem reino</div>');
        }

        profileContainer.innerHTML = fragments.join('');
    };

    const fetchProfile = async () => {
        const username = usernameInput.value.trim();
        if (!username) {
            showStatus('Informe um nome para buscar.', 'danger');
            return;
        }

        showStatus('Consultando dados...', 'warning');

        try {
            const response = await fetch(`/api/user?username=${encodeURIComponent(username)}`);
            const payload = await response.json();

            if (!response.ok) {
                showStatus(payload.message || 'Falha ao consultar usuário.', 'danger');
                return;
            }

            hideStatus();
            renderProfile(payload);
        } catch (error) {
            console.error('Erro ao buscar perfil', error);
            showStatus('Erro inesperado ao buscar perfil.', 'danger');
        }
    };

    mapButton?.addEventListener('click', handleMapFullscreen);
    loginButton.addEventListener('click', fetchProfile);
    usernameInput.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            fetchProfile();
        }
    });
})();
