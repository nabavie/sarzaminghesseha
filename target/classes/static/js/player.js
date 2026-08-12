// Audio player enhancements for the tale detail page:
// resumes from the saved position, gently nudges guests to sign up after
// two distinct free listens, reports progress for logged-in users, and
// continues to the next tale on the same <audio> element so a background
// tab can keep playing (full page navigation would be blocked by autoplay).
(function () {
    'use strict';

    var player = document.getElementById('talePlayer');
    if (!player) return;

    var taleId = parseInt(player.dataset.taleId, 10);
    var resume = parseInt(player.dataset.resumeSeconds || '0', 10);
    var authenticated = player.dataset.authenticated === 'true';
    var FREE_LIMIT = 2;
    var STORAGE_KEY = 'sq_guest_heard_tales';
    var HEAR_AFTER_SECONDS = 5;
    var AUTO_NEXT_SECONDS = 5;
    var AUTO_NEXT_KEY = 'sq_auto_next';
    var PERSIAN_DIGITS = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];
    var lastSent = -1;
    var lastTick = 0;
    var advancing = false;

    function faDigits(value) {
        return String(value).replace(/[0-9]/g, function (d) { return PERSIAN_DIGITS[+d]; });
    }

    if (resume > 0) {
        player.addEventListener('loadedmetadata', function onResume() {
            if (isFinite(player.duration) && resume < player.duration - 5) {
                player.currentTime = resume;
            }
            player.removeEventListener('loadedmetadata', onResume);
        });
    }

    function readHeard() {
        try {
            var raw = localStorage.getItem(STORAGE_KEY);
            var arr = raw ? JSON.parse(raw) : [];
            if (!Array.isArray(arr)) return [];
            return arr.map(Number).filter(function (n) { return n > 0; });
        } catch (e) {
            return [];
        }
    }

    function writeHeard(ids) {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(ids));
        } catch (e) { /* quota / private mode — ignore */ }
    }

    function markHeard(id) {
        var ids = readHeard();
        if (ids.indexOf(id) === -1) {
            ids.push(id);
            writeHeard(ids);
        }
    }

    function needsSignupGate(id) {
        var heard = readHeard();
        return heard.indexOf(id) === -1 && heard.length >= FREE_LIMIT;
    }

    if (!authenticated && taleId) {
        var modalEl = document.getElementById('guestSignupModal');
        var continueBtn = document.getElementById('guestContinueListen');
        var allowingPlay = false;
        var heardMarked = false;
        var modal = null;
        if (modalEl && window.bootstrap && bootstrap.Modal) {
            modal = bootstrap.Modal.getOrCreateInstance(modalEl);
        }

        player.addEventListener('play', function () {
            if (allowingPlay || !needsSignupGate(taleId)) return;
            player.pause();
            if (modal) {
                modal.show();
            }
        });

        player.addEventListener('timeupdate', function () {
            if (heardMarked || player.currentTime < HEAR_AFTER_SECONDS) return;
            heardMarked = true;
            markHeard(taleId);
        });

        if (continueBtn) {
            continueBtn.addEventListener('click', function () {
                allowingPlay = true;
                if (modal) modal.hide();
                var playPromise = player.play();
                if (playPromise && typeof playPromise.catch === 'function') {
                    playPromise.catch(function () { /* autoplay policies */ });
                }
            });
        }
    }

    // Arriving from an auto-next hop: browsers may still refuse to play without a
    // gesture on this page, so fall back to a visible prompt instead of silence.
    if (player.dataset.autoplay === 'true') {
        var blockedNotice = document.getElementById('autoplayBlocked');
        if (blockedNotice) {
            player.addEventListener('play', function () {
                blockedNotice.classList.add('d-none');
            });
        }
        var autoPlayPromise = player.play();
        if (autoPlayPromise && typeof autoPlayPromise.catch === 'function') {
            autoPlayPromise.catch(function () {
                if (blockedNotice) blockedNotice.classList.remove('d-none');
            });
        }
    }

    setUpAutoNext();
    setUpProgress();

    /**
     * When a tale finishes, continue on the same audio element. Navigating to a
     * new page would lose the user-gesture and fail in a minimized tab.
     * Guests are left out on purpose: they have a two-tale free limit.
     */
    function setUpAutoNext() {
        if (!authenticated) return;

        var section = document.getElementById('relatedTales');
        var nextId = section && section.getAttribute('data-next-id');
        var nextTitle = section && section.getAttribute('data-next-title');
        if (!nextId) return;

        var toggle = document.getElementById('autoNextToggle');
        var banner = document.getElementById('autoNextBanner');
        var titleEl = document.getElementById('autoNextTitle');
        var countEl = document.getElementById('autoNextCountdown');
        var cancelBtn = document.getElementById('autoNextCancel');
        var countdown = null;

        function cancel() {
            if (countdown) {
                clearInterval(countdown);
                countdown = null;
            }
            if (banner) {
                banner.classList.add('d-none');
                banner.classList.remove('d-flex');
            }
        }

        function applyTale(data) {
            taleId = data.id;
            lastSent = -1;
            lastTick = 0;
            player.dataset.taleId = String(data.id);
            player.src = data.audioUrl;
            nextId = data.nextId ? String(data.nextId) : '';
            nextTitle = data.nextTitle || '';
            if (section) {
                if (nextId) {
                    section.setAttribute('data-next-id', nextId);
                    section.setAttribute('data-next-title', nextTitle);
                } else {
                    section.removeAttribute('data-next-id');
                    section.removeAttribute('data-next-title');
                }
            }
            document.title = data.title + ' | سرزمین قصه‌ها';
            var heading = document.getElementById('taleTitle');
            if (heading) heading.textContent = data.title;
            var crumb = document.getElementById('taleBreadcrumb');
            if (crumb) crumb.textContent = data.title;
            var share = document.getElementById('shareTale');
            if (share) {
                share.setAttribute('data-share-title', data.title);
                share.setAttribute('data-share-url', window.location.origin + '/tales/' + data.id);
            }
            var cover = document.getElementById('taleCover');
            if (cover && data.coverUrl) {
                cover.src = data.coverUrl;
                cover.alt = 'جلد قصه «' + data.title + '»';
            }
            if (window.history && history.pushState) {
                history.pushState({ taleId: data.id }, data.title, '/tales/' + data.id);
            }
            if (navigator.mediaSession && window.MediaMetadata) {
                var meta = { title: data.title, artist: 'سرزمین قصه‌ها' };
                if (data.coverUrl) {
                    meta.artwork = [{ src: data.coverUrl }];
                }
                navigator.mediaSession.metadata = new MediaMetadata(meta);
            }
            var blocked = document.getElementById('autoplayBlocked');
            if (blocked) blocked.classList.add('d-none');
        }

        function goNext() {
            cancel();
            if (advancing || !nextId) return;
            advancing = true;
            var targetId = nextId;
            fetch('/api/tales/' + targetId + '/play')
                .then(function (response) {
                    if (!response.ok) throw new Error('play');
                    return response.json();
                })
                .then(function (data) {
                    applyTale(data);
                    var playPromise = player.play();
                    if (playPromise && typeof playPromise.catch === 'function') {
                        playPromise.catch(function () {
                            var blocked = document.getElementById('autoplayBlocked');
                            if (blocked) blocked.classList.remove('d-none');
                        });
                    }
                })
                .catch(function () {
                    window.location.href = '/tales/' + targetId;
                })
                .then(function () {
                    advancing = false;
                });
        }

        function startCountdown() {
            if (toggle && !toggle.checked) return;
            if (!nextId || advancing) return;
            cancel();
            // Minimized / background tabs cannot show the cancel banner, and
            // browsers throttle timers there — continue immediately.
            if (document.hidden) {
                goNext();
                return;
            }
            var remaining = AUTO_NEXT_SECONDS;
            if (titleEl) titleEl.textContent = nextTitle || '';
            if (countEl) countEl.textContent = faDigits(remaining);
            if (banner) {
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
            countdown = setInterval(function () {
                remaining -= 1;
                if (countEl) countEl.textContent = faDigits(Math.max(remaining, 0));
                if (remaining <= 0) {
                    goNext();
                }
            }, 1000);
        }

        if (toggle) {
            try {
                if (localStorage.getItem(AUTO_NEXT_KEY) === 'off') toggle.checked = false;
            } catch (e) { /* private mode — keep the default */ }
            toggle.addEventListener('change', function () {
                try {
                    localStorage.setItem(AUTO_NEXT_KEY, toggle.checked ? 'on' : 'off');
                } catch (e) { /* ignore */ }
                if (!toggle.checked) cancel();
            });
        }
        if (cancelBtn) cancelBtn.addEventListener('click', cancel);
        // replaying the finished tale means the listener is not done with it yet
        player.addEventListener('play', function () {
            if (!advancing) cancel();
        });

        player.addEventListener('ended', startCountdown);

        document.addEventListener('visibilitychange', function () {
            if (document.hidden && countdown) {
                goNext();
            }
        });

        window.addEventListener('popstate', function () {
            window.location.reload();
        });

        if (navigator.mediaSession) {
            try {
                navigator.mediaSession.setActionHandler('nexttrack', goNext);
            } catch (e) { /* handler not supported */ }
        }
    }

    function setUpProgress() {
        if (!authenticated || !taleId) return;

        var csrfMeta = document.querySelector('meta[name="_csrf"]');
        var csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

        function send(finished) {
            var seconds = Math.floor(player.currentTime || 0);
            if (!finished && seconds === lastSent) return;
            lastSent = seconds;

            var headers = { 'Content-Type': 'application/json' };
            if (csrfMeta && csrfHeaderMeta) {
                headers[csrfHeaderMeta.content] = csrfMeta.content;
            }
            var duration = isFinite(player.duration) ? Math.floor(player.duration) : null;
            fetch('/api/progress', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({
                    taleId: taleId,
                    seconds: finished ? 0 : seconds,
                    duration: duration,
                    finished: !!finished
                })
            }).catch(function () { /* progress is best-effort */ });
        }

        player.addEventListener('timeupdate', function () {
            var now = Date.now();
            if (now - lastTick >= 10000) {
                lastTick = now;
                send(false);
            }
        });
        player.addEventListener('pause', function () {
            if (advancing) return;
            send(false);
        });
        player.addEventListener('ended', function () { send(true); });
    }
})();
