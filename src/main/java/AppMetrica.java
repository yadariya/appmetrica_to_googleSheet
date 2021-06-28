import java.io.*;
import java.net.*;
import java.util.*;

public class AppMetrica {
    public static String HTTPRequest(String date1, String date2, int flag) throws IOException {
        URL url;
        Map<String, String> parameters = new HashMap<>();
        parameters.put("ids", "1485194");
        parameters.put("metrics", "ym:ce2:fgEvents,norm(ym:ce2:fgEvents),ym:ce2:usersWithEvent,norm(ym:ce2:usersWithEvent),ym:ce2:eventsPerUser,ym:ce2:usersPercent,ym:ce2:hasParamsLevel5");
        parameters.put("date1", date1);
        parameters.put("date2", date2);
        String filters = "";
        if (flag == 0) {
            filters = "ym:ce2:eventLabel==\'Cheques event\' AND ym:ce2:paramsLevel1==\'Add cheque\' AND ym:ce2:paramsLevel2==\'LoadingBanner\' AND ym:ce2:paramsLevel3==\'Skip\'";
        } else {
            filters = "ym:ce2:eventLabel==\'Cheques event\' AND ym:ce2:paramsLevel1==\'Add cheque\' AND ym:ce2:paramsLevel2==\'LoadingBanner\' AND ym:ce2:paramsLevel3==\'Accept\'";
        }
        parameters.put("filters", filters);
        parameters.put("dimensions", "ym:ce2:eventLabel,ym:ce2:paramsLevel1,ym:ce2:paramsLevel2,ym:ce2:paramsLevel3,ym:ce2:paramsLevel4");
        String urlParams = ParameterStringBuilder.getParamsString(parameters);

        url = new URL("https://api.appmetrica.yandex.ru/stat/v1/data?" + urlParams);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "OAuth AQAEA7qjwp_mAAcwuZ8fQOdpT0r0up4PANA0F90");


        int status = con.getResponseCode();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
        String data = content.toString();
        int firstSubstring = data.indexOf("\"data\"");
        data = data.substring(firstSubstring + 7);
        return data;
    }

    public static ArrayList<Banner> statsParser(String date1, String date2) throws IOException {
        //"2021-06-07" - string date format
        String data = HTTPRequest(date1, date2, 0);
        ArrayList<String> dividedDataSkip = new ArrayList<>();

        while (data.contains("\"dimensions\"")) {
            int dims = data.indexOf("\"dimensions\"");
            int dims2 = data.indexOf("\"dimensions\"", dims + 10);
            if (dims2 == -1) dims2 = data.length() - 1;
            String chunk = data.substring(dims, dims2 - 2);
            dividedDataSkip.add(chunk);
            data = data.substring(dims2);
        }

        ArrayList<Banner> banners = new ArrayList<>();
        for (int i = 0; i < dividedDataSkip.size(); i++) {
            dividedDataSkip.set(i, dividedDataSkip.get(i).substring(111));
            int endOfIndex = dividedDataSkip.get(i).indexOf('"');
            String index = dividedDataSkip.get(i).substring(0, endOfIndex);
            dividedDataSkip.set(i, dividedDataSkip.get(i).substring(dividedDataSkip.get(i).indexOf("metrics")));
            int startOfStats = dividedDataSkip.get(i).indexOf('[');
            int endOfStats = dividedDataSkip.get(i).indexOf(']');
            String stats = dividedDataSkip.get(i).substring(startOfStats + 1, endOfStats);
            String SkipEvents = stats.substring(0, stats.indexOf('.'));
            stats = stats.substring(stats.indexOf('.') + 3, stats.length() - 1);
            stats = stats.substring(stats.indexOf('.') + 1);
            String SkipUsers = stats.substring(stats.indexOf(',') + 1, stats.indexOf('.'));
            int ind = Integer.parseInt(index);
            int events = Integer.parseInt(SkipEvents);
            int users = Integer.parseInt(SkipUsers);
            banners.add(new Banner(ind));
            banners.get(i).skipEvent = events;
            banners.get(i).skipUsers = users;
        }

        String dataAccept = HTTPRequest(date1, date2, 1);
        ArrayList<String> dividedDataAccept = new ArrayList<>();

        while (dataAccept.contains("\"dimensions\"")) {
            int dims = dataAccept.indexOf("\"dimensions\"");
            int dims2 = dataAccept.indexOf("\"dimensions\"", dims + 10);
            if (dims2 == -1) dims2 = dataAccept.length() - 1;
            String chunk = dataAccept.substring(dims, dims2 - 2);
            dividedDataAccept.add(chunk);
            dataAccept = dataAccept.substring(dims2);
        }

        for (int i = 0; i < dividedDataAccept.size(); i++) {
            dividedDataAccept.set(i, dividedDataAccept.get(i).substring(113));
            int endOfIndex = dividedDataAccept.get(i).indexOf('"');
            String index = dividedDataAccept.get(i).substring(0, endOfIndex);
            dividedDataAccept.set(i, dividedDataAccept.get(i).substring(dividedDataAccept.get(i).indexOf("metrics")));
            int startOfStats = dividedDataAccept.get(i).indexOf('[');
            int endOfStats = dividedDataAccept.get(i).indexOf(']');
            String stats = dividedDataAccept.get(i).substring(startOfStats + 1, endOfStats);
            String SkipEvents = stats.substring(0, stats.indexOf('.'));
            stats = stats.substring(stats.indexOf('.') + 3, stats.length() - 1);
            stats = stats.substring(stats.indexOf('.') + 1);
            String SkipUsers = stats.substring(stats.indexOf(',') + 1, stats.indexOf('.'));
            int ind = Integer.parseInt(index);
            int events = Integer.parseInt(SkipEvents);
            int users = Integer.parseInt(SkipUsers);
            int exists = 0;
            for (Banner banner : banners) {
                if (ind == banner.index) {
                    exists = 1;
                    banner.acceptEvent = events;
                    banner.acceptUsers = users;
                }
            }
            if (exists != 1) {
                banners.add(new Banner(ind));
                banners.get(i).skipEvent = 0;
                banners.get(i).skipUsers = 0;
                banners.get(i).acceptEvent = events;
                banners.get(i).acceptUsers = users;
            }
        }
        return banners;
    }

    public static void BannerSorter(ArrayList<Banner> banners) {
        for (int i = 0; i < banners.size(); i++) {
            for (int j = i + 1; j < banners.size(); j++) {
                if (banners.get(j).index < banners.get(i).index) {
                    Banner temporary = banners.get(i);
                    banners.set(i, banners.get(j));
                    banners.set(j, temporary);
                }
            }
        }
    }
}
