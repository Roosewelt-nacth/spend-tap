const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('api', {
  minimize: () => ipcRenderer.send('window-minimize'),
  close: () => ipcRenderer.send('window-close'),
  togglePin: (pinned) => ipcRenderer.send('window-toggle-pin', pinned),

  getConfig: () => ipcRenderer.invoke('config:get'),
  saveConfig: (cfg) => ipcRenderer.invoke('config:save', cfg),

  addEntry: (entry) => ipcRenderer.invoke('entries:add', entry),
  listEntries: (opts) => ipcRenderer.invoke('entries:list', opts),

  getAdvice: (payload) => ipcRenderer.invoke('advice:get', payload),
});
