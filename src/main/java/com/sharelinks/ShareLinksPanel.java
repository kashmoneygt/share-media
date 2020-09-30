package com.sharelinks;

import com.sharelinks.models.LinkItem;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;

@Slf4j
@Singleton
public class ShareLinksPanel extends PluginPanel {

    private static final ImageIcon DEFAULT_SPOTIFY_ICON;

    private static final Color DEFAULT_BACKGROUND = new Color(11, 30, 41);
    private static final Color SPOTIFY_BACKGROUND = new Color(15, 15, 15);

    private static final int CONTENT_WIDTH = 148;
    private static final int TIME_WIDTH = 20;

    static {
        DEFAULT_SPOTIFY_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(ShareLinksPanel.class, "spotify.png"));
    }

    private final ShareLinksConfig config;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    @Inject
    ShareLinksPanel(ShareLinksConfig config) {
        super();
        this.config = config;

        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new GridLayout(0, 1, 0, 4));
    }

    public void addItemToPanel(LinkItem item) {
        // Layout of an item:
        // |--------------------------|
        // ||----| HEADER (TYPE)      |
        // ||-AV-|   --CONTENT--      |
        // ||----|      (NAME)        |
        // |--------------------------|

        // Define full panel
        JPanel fullPanel = new JPanel(new BorderLayout());
        fullPanel.setPreferredSize(new Dimension(0, 56));

        // Define icon
        JLabel icon = new JLabel();
        icon.setPreferredSize(new Dimension(64, 64));
        icon.setBorder(new EmptyBorder(0, 0, 0, 0));

        switch (item.getType()) {
            case SPOTIFY_LINK:
                if (item.getIcon() != null) {
                    icon.setIcon(item.getIcon());
                } else {
                    icon.setIcon(DEFAULT_SPOTIFY_ICON);
                }
                fullPanel.setBackground(SPOTIFY_BACKGROUND);
                break;
        }

        // Define panel to contain header and content
        JPanel headerAndContent = new JPanel();
        headerAndContent.setLayout(new BoxLayout(headerAndContent, BoxLayout.Y_AXIS));
        headerAndContent.setBorder(new EmptyBorder(4, 8, 4, 4));
        headerAndContent.setBackground(null);

        // Define header, which contains item type and timestamp
        JPanel header = new JPanel();
        header.setLayout(new BorderLayout());
        header.setBackground(null);

        Color darkerForeground = UIManager.getColor("Label.foreground").darker();

        // Define item type
        JLabel typeLabel = new JLabel(item.getType().value);
        typeLabel.setFont(FontManager.getRunescapeFont());
        typeLabel.setBackground(null);
        typeLabel.setForeground(darkerForeground);
        typeLabel.setPreferredSize(new Dimension(CONTENT_WIDTH - TIME_WIDTH, 0));

        // Define timestamp
        JLabel timeLabel = new JLabel(dateTimeFormatter.format(item.getTimestamp()));
        timeLabel.setFont(FontManager.getRunescapeFont());
        timeLabel.setForeground(darkerForeground);

        header.add(typeLabel, BorderLayout.WEST);
        header.add(timeLabel, BorderLayout.EAST);

        // Define content panel, which contains item name for now
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(null);

        // Define content
        JLabel contentLabel = new JLabel(item.getTitle());
        contentLabel.setBorder(new EmptyBorder(2, 0, 0, 0));
        contentLabel.setFont(FontManager.getRunescapeBoldFont());
        contentLabel.setForeground(darkerForeground);

        content.add(contentLabel, BorderLayout.CENTER);

        // Set headerAndContent panel as parent of header and content panels
        headerAndContent.add(header);
        headerAndContent.add(content);
        headerAndContent.add(new Box.Filler(new Dimension(0, 0),
                new Dimension(0, Short.MAX_VALUE),
                new Dimension(0, Short.MAX_VALUE)));

        // Set full panel as parent of icon and headerAndContent panels
        fullPanel.add(icon, BorderLayout.WEST);
        fullPanel.add(headerAndContent, BorderLayout.CENTER);

        Color backgroundColor = fullPanel.getBackground();
        Color hoverColor = backgroundColor.brighter().brighter();
        Color pressedColor = hoverColor.brighter();

        fullPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                fullPanel.setBackground(hoverColor);
                fullPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                fullPanel.setBackground(backgroundColor);
                fullPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                fullPanel.setBackground(pressedColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                fullPanel.setBackground(hoverColor);
                LinkBrowser.browse(item.getUrl());
            }
        });

        // add to the very top of, revalidate, and repaint the plugin panel
        add(fullPanel, 0);
        revalidate();
        repaint();
    }
}
