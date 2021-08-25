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
import java.util.*;

/**
 * before working with this program you need to go to https://developers.google.com/workspace/guides/create-credentials
 * and create your credentials. Download credentials.json and add it to resource folder
 */
public class Main {
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    static final String spreadsheetId = ""; //id of google sheet


    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = SheetsRead.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * method writeWeek adds new week in sheet
     */
    public static void writeWeek(String start, String end, Sheets service, String rangeStart, String rangeEnd) throws IOException {
        String total = start + " - " + end;
        List<List<Object>> values = Collections.singletonList(Collections.singletonList(total));
        ValueRange body = new ValueRange()
                .setValues(values);
        AppendValuesResponse result =
                service.spreadsheets().values().append(spreadsheetId, rangeStart + ":" + rangeEnd, body)
                        .setValueInputOption("USER_ENTERED")
                        .execute();
    }

    /**
     * method writeEventName prints the headers ACCEPT SKIP CTR in sheet
     */
    public static void writeEventName(Sheets service, String rangeStart, String rangeEnd) throws IOException {
        List<List<Object>> values = Collections.singletonList(Arrays.asList("Accept", "Skip", "CTR"));
        ValueRange body = new ValueRange()
                .setValues(values);
        AppendValuesResponse result =
                service.spreadsheets().values().append(spreadsheetId, rangeStart + ":" + rangeEnd, body)
                        .setValueInputOption("USER_ENTERED")
                        .execute();
    }

    /**
     * method writeValues write 3 values (ACCEPT, SKIP, CTR) for each id in banners
     */
    public static void writeValues(Sheets service, int init_size, int diff, ArrayList<Banner> banners, String rangeStart) throws IOException, InterruptedException {
        for (int i = 1; i <= init_size; i++) {
            if (i != banners.get(i - diff).index) {
                init_size++;
                diff++;
                List<List<Object>> values = Collections.singletonList(Collections.singletonList("inactive"));
                ValueRange body = new ValueRange()
                        .setValues(values);
                String range = rangeStart + (i + 3);
                AppendValuesResponse result =
                        service.spreadsheets().values().append(spreadsheetId, range, body)
                                .setValueInputOption("USER_ENTERED")
                                .execute();
            } else {
                int acceptEvents = banners.get(i - diff).acceptEvent;
                int skipEvents = banners.get(i - diff).skipEvent;
                double CTR = acceptEvents * 0.1 / (skipEvents + acceptEvents) * 1000;
                String range = rangeStart + (i + 3);
                DecimalFormat dec = new DecimalFormat("#0.00");
                List<List<Object>> values = Collections.singletonList(Arrays.asList(String.valueOf(acceptEvents),
                        String.valueOf(skipEvents), dec.format(CTR) + "%"));
                ValueRange body = new ValueRange()
                        .setValues(values);
                AppendValuesResponse result =
                        service.spreadsheets().values().append(spreadsheetId, range, body)
                                .setValueInputOption("USER_ENTERED")
                                .execute();
            }
            if (i % 10 == 0) {
                Thread.sleep(10000);
            }
        }
    }

    public static void main(String[] args) throws GeneralSecurityException, IOException, InterruptedException {
        Scanner scn = new Scanner(System.in);
        System.out.println("Enter start date in format YYYY-MM-DD");
        String week_starts = scn.nextLine();
        System.out.println("Enter end date in format YYYY-MM-DD");
        String week_ends = scn.nextLine();
        System.out.println("Enter the start of range with capital letters");
        String rangeStart = scn.nextLine();
        System.out.println("Enter the end of range with capital letters");
        String rangeEnd = scn.nextLine();
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        writeWeek(week_starts, week_ends, service, rangeStart, rangeEnd);
        writeEventName(service, rangeStart, rangeEnd);
        ArrayList<Banner> banners = AppMetrica.statsParser(week_starts, week_ends);
        AppMetrica.BannerSorter(banners);
        int init_size = banners.size();
        int diff = 1; //initial difference between index of banner and index of list
        writeValues(service, init_size, diff, banners, rangeStart);
    }
}
