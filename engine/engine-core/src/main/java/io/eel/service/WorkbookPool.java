package io.eel.service;

import io.eel.model.proxy.WorkbookProxy;

public interface WorkbookPool {

    WorkbookProxy getWorkbookProxy();

    void releaseWorkbookProxy(WorkbookProxy workbookProxy);

}
