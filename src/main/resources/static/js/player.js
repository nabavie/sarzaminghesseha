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
