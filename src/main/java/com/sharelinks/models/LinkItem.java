package com.sharelinks.models;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.time.LocalDateTime;

@Data
@RequiredArgsConstructor
public class LinkItem {
    private final LinkItemType type;
    private final ImageIcon icon;
    private final String title;
    private final String content;
    private final String url;
    private final LocalDateTime timestamp;
}
