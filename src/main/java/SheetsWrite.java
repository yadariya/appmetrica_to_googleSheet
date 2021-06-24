import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.*;
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SheetsWrite {
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    static final String spreadsheetId = "1Ymt4EVASN4Fl4v2r7bsffbFK8PixN2VypUYj7li6R-o";


    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = SheetsRead.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static void writeWeek(String start, String end, Sheets service) throws IOException {
        String total = start + " - " + end;
        List<List<Object>> values = Collections.singletonList(Collections.singletonList(total));
        ValueRange body = new ValueRange()
                .setValues(values);
        AppendValuesResponse result =
                service.spreadsheets().values().append(spreadsheetId, "L1:N1", body)
                        .setValueInputOption("USER_ENTERED")
                        .execute();
    }

    public static void writeEventName(Sheets service) throws IOException {
        List<List<Object>> values = Collections.singletonList(Arrays.asList("Accept", "Skip", "CTR"));
        ValueRange body = new ValueRange()
                .setValues(values);
        AppendValuesResponse result =
                service.spreadsheets().values().append(spreadsheetId, "L2:N2", body)
                        .setValueInputOption("USER_ENTERED")
                        .execute();
    }

    public static void writeValues(Sheets service, int init_size, int diff, ArrayList<Banner> banners) throws IOException, InterruptedException {
        for (int i = 1; i <= init_size; i++) {
            if (i != banners.get(i - diff).index) {
                init_size++;
                diff++;
                List<List<Object>> values = Collections.singletonList(Collections.singletonList("inactive"));
                ValueRange body = new ValueRange()
                        .setValues(values);
                String range = "L" + (i + 3);
                AppendValuesResponse result =
                        service.spreadsheets().values().append(spreadsheetId, range, body)
                                .setValueInputOption("USER_ENTERED")
                                .execute();
            } else {
                int acceptEvents = banners.get(i - diff).acceptEvent;
                int skipEvents = banners.get(i - diff).skipEvent;
                double CTR = acceptEvents * 0.1 / (skipEvents + acceptEvents) * 1000;
                String range = "L" + (i + 3);
                DecimalFormat dec = new DecimalFormat("#0.00");
                List<List<Object>> values = Collections.singletonList(Arrays.asList(acceptEvents,
                        skipEvents, dec.format(CTR) + "%"));
                ValueRange body = new ValueRange()
                        .setValues(values);
                AppendValuesResponse result =
                        service.spreadsheets().values().append(spreadsheetId, range, body)
                                .setValueInputOption("USER_ENTERED")
                                .execute();
                //write banner.get(i-diff).skipEvent banner.get(i-diff).acceptEvent etc into i+height

            }
            if (i % 10 == 0) {
                Thread.sleep(10000);
            }
        }

    }

    public static void main(String[] args) throws GeneralSecurityException, IOException, InterruptedException {
        String week_starts = "2021-01-18";
        String week_ends = "2021-01-24";
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        writeWeek(week_starts, week_ends, service);
        writeEventName(service);
        ArrayList<Banner> banners = Main.statsParser(week_starts, week_ends);
        Main.BannerSorter(banners);
        int init_size = banners.size();
        int diff = 1; //initial difference between index of banner and index of list
        writeValues(service, init_size, diff, banners);
    }
}
