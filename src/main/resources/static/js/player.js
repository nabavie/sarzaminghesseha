// Audio player enhancements for the tale detail page:
// resumes from the saved position, gently nudges guests to sign up after
// two distinct free listens, and reports progress for logged-in users.
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

    function faDigits(value) {
        return String(value).replace(/[0-9]/g, function (d) { return PERSIAN_DIGITS[+d]; });
    }

    if (resume > 0) {
        player.addEventListener('loadedmetadata', function () {
            if (isFinite(player.duration) && resume < player.duration - 5) {
                player.currentTime = resume;
            }
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

    /**
     * When a tale finishes, offer the first related tale after a short countdown.
     * Guests are left out on purpose: they have a two-tale free limit, and hopping
     * them onto a third tale would just hit the signup modal.
     */
    function setUpAutoNext() {
        if (!authenticated) return;

        var section = document.getElementById('relatedTales');
        var next = section && section.querySelector('[data-related-id]');
        if (!next) return;

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
        player.addEventListener('play', cancel);

        player.addEventListener('ended', function () {
            if (toggle && !toggle.checked) return;
            cancel();

            var remaining = AUTO_NEXT_SECONDS;
            if (titleEl) titleEl.textContent = next.getAttribute('data-related-title') || '';
            if (countEl) countEl.textContent = faDigits(remaining);
            if (banner) {
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
            countdown = setInterval(function () {
                remaining -= 1;
                if (countEl) countEl.textContent = faDigits(Math.max(remaining, 0));
                if (remaining <= 0) {
                    clearInterval(countdown);
                    countdown = null;
                    window.location.href = '/tales/' + next.getAttribute('data-related-id') + '?autoplay=1';
                }
            }, 1000);
        });
    }

    if (!authenticated || !taleId) return;

    var csrfMeta = document.querySelector('meta[name="_csrf"]');
    var csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    var lastSent = -1;

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

    var lastTick = 0;
    player.addEventListener('timeupdate', function () {
        var now = Date.now();
        if (now - lastTick >= 10000) { // every 10 seconds while playing
            lastTick = now;
            send(false);
        }
    });
    player.addEventListener('pause', function () { send(false); });
    player.addEventListener('ended', function () { send(true); });
})();
