package com.sharelinks.utilities;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class ClipboardUtility {

    public String GetStringFromClipboard() {
        String clipboardString;
        try {
            clipboardString = Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getContents(null)
                    .getTransferData(DataFlavor.stringFlavor).toString();
        } catch (Exception e) {
            log.warn("[External Plugin][Share Links] Error reading user's clipboard.");
            clipboardString = "";
        }

        return clipboardString;
    }

    public String GetStringStartingWithSubstringFromClipboard(String substring, int secondsToWait) throws InterruptedException {
        int elapsed = 0;
        do {
            if (GetStringFromClipboard().startsWith(substring)) {
                return GetStringFromClipboard();
            }
            TimeUnit.SECONDS.sleep(1);
            elapsed++;
        } while (elapsed < secondsToWait);
        return "";
    }
}
