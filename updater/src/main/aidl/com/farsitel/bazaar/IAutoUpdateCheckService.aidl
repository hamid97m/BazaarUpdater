package com.farsitel.bazaar;
interface IAutoUpdateCheckService {
    boolean isAutoUpdateEnabled(String packageName);
    boolean isUpdateDownloaded(String packageName);
}