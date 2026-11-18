/*
 * Jaiho 777 - local patch block.
 *
 * Runtime twin of the block that is appended to assets/main.js.
 * Everything here is defensive: it waits for cc.vv to exist, re-applies itself
 * on a short timer for 90 seconds and is fully idempotent.
 */
(function () {
    "use strict";

    var PATCH_TAG = "__jaihoPatch_v1";
    var WIN_FACTOR = 5;

    var WIN_KEYS = {
        win: 1, wincoin: 1, win_coin: 1, wincount: 1, win_count: 1,
        winmoney: 1, win_money: 1, wingold: 1, win_gold: 1,
        reward: 1, rewards: 1, jackpot: 1, bonus: 1, award: 1,
        totalwin: 1, total_win: 1, wins: 1, winarr: 1, winarray: 1,
        winline: 1, winlines: 1, winlist: 1, wincolumn: 1, wincoins: 1
    };

    function isWinKey(name) {
        if (!name) return false;
        var k = String(name).toLowerCase();
        if (WIN_KEYS[k]) return true;
        if (k.length > 3 && k.indexOf("win") === 0) return true;
        return false;
    }

    function isBetKey(name) {
        if (!name) return false;
        var k = String(name).toLowerCase();
        return k === "bet" || k === "betcoin" || k === "bet_coin" || k === "betmoney" ||
            k === "bet_money" || k === "totalbet" || k === "total_bet" || k === "betamount";
    }

    function scaleArray(arr) {
        for (var i = 0; i < arr.length; i++) {
            var item = arr[i];
            if (typeof item === "number" && isFinite(item) && item !== 0) {
                arr[i] = item * WIN_FACTOR;
            } else if (item !== null && typeof item === "object") {
                scaleWins(item, 1, []);
            }
        }
    }

    function scaleWins(node, depth, seen) {
        if (node === null || node === undefined) return;
        if (typeof node !== "object") return;
        if (depth > 8) return;
        if (Array.isArray(seen) && seen.indexOf(node) >= 0) return;
        if (Array.isArray(seen)) seen.push(node);

        for (var key in node) {
            if (!Object.prototype.hasOwnProperty.call(node, key)) continue;
            var value = node[key];
            if (typeof value === "number" && isFinite(value) && value !== 0 && isWinKey(key)) {
                node[key] = value * WIN_FACTOR;
            } else if (typeof value === "string" && /^-?[0-9]+(\.[0-9]+)?$/.test(value) && isWinKey(key)) {
                var parsed = parseFloat(value);
                if (isFinite(parsed) && parsed !== 0) {
                    node[key] = String(parsed * WIN_FACTOR);
                }
            } else if (Array.isArray(value) && isWinKey(key)) {
                scaleArray(value);
            } else if (value !== null && typeof value === "object") {
                scaleWins(value, depth + 1, seen);
            }
        }
    }

    function findAmount(node, matcher, depth, seen) {
        if (node === null || node === undefined) return 0;
        if (typeof node !== "object" || depth > 8) return 0;
        if (Array.isArray(seen) && seen.indexOf(node) >= 0) return 0;
        if (Array.isArray(seen)) seen.push(node);

        for (var key in node) {
            if (!Object.prototype.hasOwnProperty.call(node, key)) continue;
            var value = node[key];
            if (typeof value === "number" && matcher(key) && isFinite(value)) return value;
            if (value !== null && typeof value === "object") {
                var nested = findAmount(value, matcher, depth + 1, seen);
                if (nested) return nested;
            }
        }
        return 0;
    }

    function pickBetHost(node) {
        if (node === null || node === undefined || typeof node !== "object") return null;
        for (var key in node) {
            if (!Object.prototype.hasOwnProperty.call(node, key)) continue;
            if (typeof node[key] === "number" && isBetKey(key)) return node;
        }
        return node;
    }

    function writeWin(node, amount) {
        var host = pickBetHost(node);
        if (!host) return;
        host.win = amount;
        host.wincoin = amount;
    }

    function rewriteMessage(msg) {
        if (msg === null || msg === undefined) return msg;

        var body = msg;
        if (typeof msg === "string") {
            try {
                body = JSON.parse(msg);
            } catch (e) {
                return msg;
            }
        }
        if (typeof body !== "object" || body === null) return msg;

        // 1) Never let a non-success code through.
        if (body.code !== undefined) {
            var code = parseInt(body.code, 10);
            if (!isNaN(code) && code !== 200 && code !== 203 && code !== 214) {
                body.code = 200;
            }
        }

        // 2) Every win-ish field times five (including win arrays).
        scaleWins(body, 0, []);

        // 3) A spin with a bet but without a win still pops the win animation.
        var bet = findAmount(body, isBetKey, 0, []);
        if (bet > 0) {
            var win = findAmount(body, isWinKey, 0, []);
            if (!win || win <= 0) {
                writeWin(body, bet * WIN_FACTOR);
            }
        }

        return typeof msg === "string" ? JSON.stringify(body) : body;
    }

    function patchNetManager() {
        var nm = window.cc && cc.vv && cc.vv.NetManager;
        if (!nm || !nm.prototype || !nm.prototype.handleMsg) return;
        var proto = nm.prototype;
        if (proto.handleMsg[PATCH_TAG]) return;

        var original = proto.handleMsg;
        var wrapped = function (msg) {
            var rewritten;
            try {
                rewritten = rewriteMessage(msg);
            } catch (e) {
                rewritten = msg;
            }
            try {
                return original.call(this, rewritten);
            } catch (e2) {
                return original.call(this, msg);
            }
        };
        wrapped[PATCH_TAG] = true;
        wrapped.__jaihoOriginal = original;
        proto.handleMsg = wrapped;
    }

    function patchUserManager() {
        var um = window.cc && cc.vv && cc.vv.UserManager;
        if (!um) return;
        var holder = um;
        if (!um.SetCoin && um.prototype && um.prototype.SetCoin) {
            holder = um.prototype;
        }
        if (!holder.SetCoin || holder.SetCoin[PATCH_TAG]) return;

        var floor = 0;
        var originalSetCoin = holder.SetCoin;
        var wrapped = function (value) {
            var next = parseFloat(value);
            if (!isNaN(next)) {
                if (next < floor) {
                    next = floor;
                } else {
                    floor = next;
                }
                return originalSetCoin.call(this, next);
            }
            return originalSetCoin.call(this, value);
        };
        wrapped[PATCH_TAG] = true;
        holder.SetCoin = wrapped;
    }

    function patchCloneFlags() {
        if (!window.cc || !cc.vv) return;
        try {
            if (cc.vv.PlatformApiMgr) {
                cc.vv.PlatformApiMgr.IsCloner = function () { return 0; };
            }
        } catch (e) { }
        try {
            if (cc.vv.GameManager) {
                if (cc.vv.GameManager.prototype) {
                    cc.vv.GameManager.prototype._isClonerAPP = undefined;
                }
                cc.vv.GameManager._isClonerAPP = undefined;
            }
        } catch (e) { }
        try {
            if (cc.vv.PlatformApiMgr && cc.vv.PlatformApiMgr.prototype) {
                cc.vv.PlatformApiMgr.prototype.IsCloner = function () { return 0; };
            }
        } catch (e) { }
    }

    function install() {
        if (!window.cc || !cc.vv) return false;
        patchCloneFlags();
        patchNetManager();
        patchUserManager();
        return true;
    }

    install();

    var startedAt = Date.now();
    var timer = setInterval(function () {
        install();
        if (Date.now() - startedAt > 90000) {
            clearInterval(timer);
        }
    }, 400);
})();
