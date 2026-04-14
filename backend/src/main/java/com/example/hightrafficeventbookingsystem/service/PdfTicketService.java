package com.example.hightrafficeventbookingsystem.service;

import com.example.hightrafficeventbookingsystem.model.Event;
import com.example.hightrafficeventbookingsystem.model.Seat;
import com.example.hightrafficeventbookingsystem.model.Ticket;
import com.example.hightrafficeventbookingsystem.model.User;
import com.example.hightrafficeventbookingsystem.model.VenueType;
import com.example.hightrafficeventbookingsystem.repository.TicketRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PdfTicketService {

    private final TicketRepository ticketRepository;

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy - HH:mm", new Locale("pl", "PL"));

    @Transactional(readOnly = true)
    public byte[] generatePdf(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findByIdWithSeatsAndEvent(ticketId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        if (!ticket.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        List<Seat> seats = ticket.getSeats();
        if (seats.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Ticket has no seats");
        }

        Event event = seats.get(0).getEvent();
        User user = ticket.getUser();

        try {
            return buildPdf(ticket, event.getName(), event.getDate(), event.getVenueType(), seats, user);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "PDF generation failed: " + e.getMessage());
        }
    }

    private byte[] buildPdf(Ticket ticket, String eventName, LocalDateTime eventDate,
                             VenueType venueType, List<Seat> seats, User user) throws Exception {

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float width = page.getMediaBox().getWidth();   // 595
            float height = page.getMediaBox().getHeight(); // 842
            float margin = 50f;
            float y = height - margin;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font fontReg  = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                // Header bar - dark navy
                cs.setNonStrokingColor(0.10f, 0.10f, 0.18f);
                cs.addRect(0, height - 60, width, 60);
                cs.fill();

                cs.beginText();
                cs.setFont(fontBold, 22);
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.newLineAtOffset(margin, height - 42);
                cs.showText("BookIt");
                cs.endText();

                y = height - 80;

                // Event name
                cs.beginText();
                cs.setFont(fontBold, 18);
                cs.setNonStrokingColor(0.1f, 0.1f, 0.18f);
                cs.newLineAtOffset(margin, y);
                cs.showText(truncate(asciify(eventName), 55));
                cs.endText();
                y -= 24;

                // Event date
                cs.beginText();
                cs.setFont(fontReg, 12);
                cs.setNonStrokingColor(0.3f, 0.3f, 0.4f);
                cs.newLineAtOffset(margin, y);
                cs.showText(asciify(eventDate.format(DATE_FMT)));
                cs.endText();
                y -= 18;

                // Venue type
                cs.beginText();
                cs.setFont(fontReg, 11);
                cs.setNonStrokingColor(0.4f, 0.4f, 0.5f);
                cs.newLineAtOffset(margin, y);
                cs.showText(friendlyVenueType(venueType));
                cs.endText();
                y -= 30;

                // Divider
                cs.setStrokingColor(0.85f, 0.85f, 0.9f);
                cs.setLineWidth(0.5f);
                cs.moveTo(margin, y);
                cs.lineTo(width - margin, y);
                cs.stroke();
                y -= 20;

                // Seats header
                cs.beginText();
                cs.setFont(fontBold, 12);
                cs.setNonStrokingColor(0.1f, 0.1f, 0.18f);
                cs.newLineAtOffset(margin, y);
                cs.showText("Twoje miejsca:");
                cs.endText();
                y -= 18;

                // Each seat line
                for (Seat seat : seats) {
                    String line = "* " + asciify(safeStr(seat.getCategory()))
                        + "  -  Rzad " + seat.getRowNumber()
                        + "  -  Miejsce " + seat.getSeatNumber()
                        + "  -  Sekcja " + asciify(safeStr(seat.getSection()))
                        + "  -  " + formatPrice(seat.getPrice()) + " PLN";

                    cs.beginText();
                    cs.setFont(fontReg, 11);
                    cs.setNonStrokingColor(0.15f, 0.15f, 0.25f);
                    cs.newLineAtOffset(margin + 10, y);
                    cs.showText(truncate(line, 80));
                    cs.endText();
                    y -= 16;
                }

                y -= 10;

                // Total price
                cs.beginText();
                cs.setFont(fontBold, 13);
                cs.setNonStrokingColor(0.1f, 0.1f, 0.18f);
                cs.newLineAtOffset(margin, y);
                cs.showText("Lacznie: " + formatPrice(ticket.getTotalPrice()) + " PLN");
                cs.endText();
                y -= 30;

                // Divider
                cs.setStrokingColor(0.85f, 0.85f, 0.9f);
                cs.moveTo(margin, y);
                cs.lineTo(width - margin, y);
                cs.stroke();
                y -= 20;

                // QR code
                BufferedImage qrImage = generateQr("BOOKIT-" + ticket.getId(), 180);
                PDImageXObject qrXObject = LosslessFactory.createFromImage(doc, qrImage);
                cs.drawImage(qrXObject, margin, y - 180, 180, 180);

                // Ticket info next to QR
                float infoX = margin + 200;
                float infoY = y - 30;

                cs.beginText();
                cs.setFont(fontBold, 12);
                cs.setNonStrokingColor(0.1f, 0.1f, 0.18f);
                cs.newLineAtOffset(infoX, infoY);
                cs.showText("Bilet #" + ticket.getId());
                cs.endText();
                infoY -= 18;

                cs.beginText();
                cs.setFont(fontReg, 10);
                cs.setNonStrokingColor(0.4f, 0.4f, 0.5f);
                cs.newLineAtOffset(infoX, infoY);
                cs.showText(asciify(safeStr(user.getEmail())));
                cs.endText();
                infoY -= 16;

                cs.beginText();
                cs.setFont(fontReg, 9);
                cs.setNonStrokingColor(0.5f, 0.5f, 0.6f);
                cs.newLineAtOffset(infoX, infoY);
                cs.showText("Wygenerowano: " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                cs.endText();

                // Footer
                cs.setNonStrokingColor(0.10f, 0.10f, 0.18f);
                cs.addRect(0, 0, width, 30);
                cs.fill();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private BufferedImage generateQr(String content, int size) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", stream);
        return ImageIO.read(new java.io.ByteArrayInputStream(stream.toByteArray()));
    }

    private String friendlyVenueType(VenueType vt) {
        if (vt == null) return "";
        return switch (vt) {
            case CINEMA -> "Kino";
            case STADIUM -> "Stadion";
            case CONCERT_ARENA -> "Arena koncertowa";
        };
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0.00";
        return price.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String safeStr(String s) {
        return s != null ? s : "";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "..." : s;
    }

    /**
     * Replace Polish-specific and other non-Latin-1 characters with ASCII equivalents
     * to prevent PDFBox Type1 font encoding errors.
     */
    private String asciify(String s) {
        if (s == null) return "";
        return s
            .replace("\u00e0", "a").replace("\u00e1", "a").replace("\u00e2", "a")
            .replace("\u00e3", "a").replace("\u00e4", "a").replace("\u00e5", "a")
            .replace("\u00c0", "A").replace("\u00c1", "A").replace("\u00c2", "A")
            .replace("\u00c3", "A").replace("\u00c4", "A").replace("\u00c5", "A")
            // Polish: a with ogonek
            .replace("\u0105", "a").replace("\u0104", "A")
            // Polish: c with acute
            .replace("\u0107", "c").replace("\u0106", "C")
            // Polish: e with ogonek
            .replace("\u0119", "e").replace("\u0118", "E")
            // Polish: l with stroke
            .replace("\u0142", "l").replace("\u0141", "L")
            // Polish: n with acute
            .replace("\u0144", "n").replace("\u0143", "N")
            // Polish: o with acute
            .replace("\u00f3", "o").replace("\u00d3", "O")
            // Polish: s with acute
            .replace("\u015b", "s").replace("\u015a", "S")
            // Polish: z with acute
            .replace("\u017a", "z").replace("\u0179", "Z")
            // Polish: z with dot above
            .replace("\u017c", "z").replace("\u017b", "Z")
            // German/other umlauts
            .replace("\u00fc", "u").replace("\u00dc", "U")
            .replace("\u00f6", "o").replace("\u00d6", "O")
            .replace("\u00e9", "e").replace("\u00e8", "e")
            .replace("\u00ea", "e").replace("\u00eb", "e")
            .replace("\u00c9", "E").replace("\u00c8", "E")
            .replace("\u00ef", "i").replace("\u00ee", "i")
            .replace("\u00ed", "i").replace("\u00ec", "i")
            // em dash / en dash -> hyphen
            .replace("\u2013", "-").replace("\u2014", "-")
            // curly quotes -> straight quotes
            .replace("\u201c", "\"").replace("\u201d", "\"")
            .replace("\u2018", "'").replace("\u2019", "'");
    }
}
