const { app, BrowserWindow, Menu, session, ipcMain, clipboard, shell } = require('electron');
const path = require('path');
const fs = require('fs');

const INSTAGRAM_ORIGIN = 'https://www.instagram.com';
const ALLOWED_HOSTS = ['instagram.com', 'www.instagram.com'];
const ALLOWED_PATHS = ['/direct', '/call', '/accounts', '/challenge', '/two_factor', '/re_login'];
const INBOX_URL = 'https://www.instagram.com/direct/inbox/';

function isInstagramOrigin(url) {
    try {
        const u = new URL(url);
        const o = u.origin;
        return o === INSTAGRAM_ORIGIN || o === 'https://instagram.com';
    } catch {
        return false;
    }
}

function isUrlAllowed(url) {
    try {
        const parsed = new URL(url);
        const host = parsed.hostname.toLowerCase();
        if (!ALLOWED_HOSTS.includes(host)) return false;
        const pathname = parsed.pathname;
        if (pathname === '/') return false;
        for (const prefix of ALLOWED_PATHS) {
            if (pathname.startsWith(prefix)) return true;
        }
        return false;
    } catch {
        return false;
    }
}

let mainWindow;
let toolsWin;

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1024,
        height: 768,
        minWidth: 800,
        minHeight: 600,
        icon: path.join(__dirname, '..', 'assets', 'icon.png'),
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: false,
            sandbox: false,
            webSecurity: true,
            allowRunningInsecureContent: false,
        }
    });

    session.defaultSession.setPermissionRequestHandler((webContents, permission, callback, details) => {
        if (!isInstagramOrigin(details.requestingUrl) && !isInstagramOrigin(details.origin)) {
            callback(false);
            return;
        }
        if (permission === 'media' || permission === 'microphone' || permission === 'camera') {
            callback(true);
        } else {
            callback(false);
        }
    });

    mainWindow.loadURL(INBOX_URL);

    mainWindow.webContents.setWindowOpenHandler(({ url }) => {
        try {
            const parsed = new URL(url);
            const origin = parsed.origin;
            if ((origin === INSTAGRAM_ORIGIN || origin === 'https://instagram.com') && parsed.pathname.startsWith('/call')) {
                const callWin = new BrowserWindow({
                    width: 1000,
                    height: 700,
                    webPreferences: {
                        preload: path.join(__dirname, 'preload.js'),
                        contextIsolation: true,
                        nodeIntegration: false,
                        sandbox: false,
                        webSecurity: true,
                        allowRunningInsecureContent: false,
                    }
                });
                callWin.loadURL(url);
                return { action: 'deny' };
            }
        } catch {}
        if (isUrlAllowed(url)) {
            mainWindow.loadURL(url);
        } else {
            mainWindow.loadURL(INBOX_URL);
        }
        return { action: 'deny' };
    });

    mainWindow.webContents.on('will-navigate', (event, url) => {
        if (!isUrlAllowed(url)) {
            event.preventDefault();
            mainWindow.loadURL(INBOX_URL);
        }
    });

    mainWindow.webContents.on('did-navigate', (event, url) => {
        if (!isUrlAllowed(url)) {
            mainWindow.loadURL(INBOX_URL);
        }
    });

    mainWindow.webContents.on('did-navigate-in-page', (event, url) => {
        if (!isUrlAllowed(url)) {
            mainWindow.loadURL(INBOX_URL);
        }
    });

    mainWindow.webContents.on('did-finish-load', () => {
        const guardPath = path.join(__dirname, 'guard.js');
        const guardCode = fs.readFileSync(guardPath, 'utf-8');
        mainWindow.webContents.executeJavaScript(guardCode).catch(() => {});
    });

    function toggleToolsWindow() {
        if (toolsWin && !toolsWin.isDestroyed()) {
            toolsWin.focus();
            return;
        }
        toolsWin = new BrowserWindow({
            width: 320,
            height: 370,
            resizable: false,
            title: 'FocusGram Tools',
            parent: mainWindow,
            webPreferences: {
                nodeIntegration: true,
                contextIsolation: false
            }
        });
        var html = '<!DOCTYPE html><html><head><meta charset="utf-8"><style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",system-ui,sans-serif;padding:16px;background:#1a1a1a;color:#e0e0e0;font-size:13px}h2{font-size:15px;font-weight:600;color:#fff;margin-bottom:14px}button{display:block;width:100%;padding:9px 14px;margin-bottom:7px;border:1px solid #333;border-radius:6px;background:#262626;color:#e0e0e0;font-size:13px;cursor:pointer;text-align:left}button:hover{background:#333;border-color:#555}#find-group{display:none;margin-bottom:7px}#find-input{width:100%;padding:8px 10px;border:1px solid #555;border-radius:6px;background:#222;color:#e0e0e0;font-size:13px}#find-input:focus{outline:none;border-color:#777}#find-status{font-size:11px;color:#999;margin-top:4px}</style></head><body><h2>FocusGram Tools</h2><button onclick="send(\'reload\')">Reload</button><div id="find-wrapper"><button onclick="toggleFind()">Find in Page</button><div id="find-group"><input id="find-input" type="text" placeholder="Search..." oninput="findText(this.value)" onkeydown="if(event.key===\'Escape\')closeFind()"><div id="find-status"></div></div></div><button onclick="send(\'clear-session\')">Clear Session / Logout</button><button onclick="send(\'open-downloads\')">Open Downloads Folder</button><button onclick="send(\'copy-url\')">Copy Current URL</button><script>var e=require(\'electron\').ipcRenderer;function send(a){e.send(\'tools:action\',a)}function toggleFind(){var g=document.getElementById(\'find-group\'),i=document.getElementById(\'find-input\');if(g.style.display===\'block\'){g.style.display=\'none\';i.value=\'\';document.getElementById(\'find-status\').textContent=\'\';e.send(\'tools:action\',\'stop-find\')}else{g.style.display=\'block\';i.focus()}}function findText(t){if(t){e.send(\'tools:find\',t)}else{e.send(\'tools:action\',\'stop-find\');document.getElementById(\'find-status\').textContent=\'\'}}function closeFind(){document.getElementById(\'find-group\').style.display=\'none\';document.getElementById(\'find-input\').value=\'\';document.getElementById(\'find-status\').textContent=\'\';e.send(\'tools:action\',\'stop-find\')}e.on(\'find-result\',function(ev,d){var s=document.getElementById(\'find-status\');if(d.matches===0){s.textContent=\'No results\'}else{s.textContent=d.activeMatchOrdinal+\' of \'+d.matches}})</script></body></html>';
        toolsWin.loadURL('data:text/html;charset=utf-8,' + encodeURIComponent(html));
        toolsWin.on('closed', function() { toolsWin = null; });
    }

    const menuTemplate = [
        {
            label: 'FocusGram',
            submenu: [
                {
                    label: 'Reload',
                    accelerator: 'CmdOrCtrl+R',
                    click: () => { mainWindow.loadURL(INBOX_URL); }
                },
                { type: 'separator' },
                {
                    label: 'Clear Session',
                    click: () => {
                        mainWindow.webContents.session.clearStorageData();
                        mainWindow.webContents.session.clearCache();
                        mainWindow.webContents.session.clearAuthCache();
                        mainWindow.loadURL(INBOX_URL);
                    }
                },
                { type: 'separator' },
                {
                    label: 'Back',
                    accelerator: 'CmdOrCtrl+Left',
                    click: () => { if (mainWindow.webContents.canGoBack()) mainWindow.webContents.goBack(); }
                },
                {
                    label: 'Forward',
                    accelerator: 'CmdOrCtrl+Right',
                    click: () => { if (mainWindow.webContents.canGoForward()) mainWindow.webContents.goForward(); }
                },
                { type: 'separator' },
                {
                    label: 'FocusGram Tools',
                    accelerator: 'CmdOrCtrl+S',
                    click: toggleToolsWindow
                },
                { type: 'separator' },
                { role: 'quit' }
            ]
        },
        {
            label: 'Edit',
            submenu: [
                { role: 'undo' },
                { role: 'redo' },
                { type: 'separator' },
                { role: 'cut' },
                { role: 'copy' },
                { role: 'paste' },
                { role: 'selectAll' }
            ]
        },
        {
            label: 'View',
            submenu: [
                { role: 'reload' },
                { role: 'toggleDevTools' },
                { type: 'separator' },
                { role: 'zoomIn' },
                { role: 'zoomOut' },
                { role: 'resetZoom' }
            ]
        }
    ];

    const menu = Menu.buildFromTemplate(menuTemplate);
    Menu.setApplicationMenu(menu);

    ipcMain.on('tools:action', (event, action) => {
        switch (action) {
            case 'reload':
                mainWindow.webContents.reload();
                break;
            case 'stop-find':
                mainWindow.webContents.stopFindInPage('clearSelection');
                break;
            case 'clear-session':
                mainWindow.webContents.session.clearStorageData();
                mainWindow.webContents.session.clearCache();
                mainWindow.webContents.session.clearAuthCache();
                mainWindow.loadURL(INBOX_URL);
                break;
            case 'open-downloads':
                shell.openPath(app.getPath('downloads'));
                break;
            case 'copy-url':
                clipboard.writeText(mainWindow.webContents.getURL());
                break;
        }
    });

    ipcMain.on('tools:find', (event, text) => {
        mainWindow.webContents.findInPage(text);
    });

    mainWindow.webContents.on('found-in-page', (event, result) => {
        if (toolsWin && !toolsWin.isDestroyed()) {
            toolsWin.webContents.send('find-result', result);
        }
    });

    mainWindow.on('closed', () => {
        mainWindow = null;
    });
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
    app.quit();
});

app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
        createWindow();
    }
});
