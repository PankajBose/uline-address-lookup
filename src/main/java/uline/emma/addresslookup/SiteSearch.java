package uline.emma.addresslookup;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SiteSearch {
    private static final Map<String, Map<String, String>> siteData = new HashMap<>();

    public static void main(String[] args) throws IOException {
        loadFromCosmosDB();

        search("global", "distribution");
    }

    private static void search(String siteName, String name) {
        long l = System.currentTimeMillis();

        Map<String, String> matchedData = new HashMap<>();

        Map<String, String> personInfo = siteData.get(siteName);
        if (personInfo != null) for (Map.Entry<String, String> entry : personInfo.entrySet()) {
            String personName = entry.getKey();
            String email = entry.getValue();

            if (personName.toLowerCase().contains(name) || email.toLowerCase().contains(name))
                matchedData.put(personName, email);
        }

        System.out.println("matchedData = " + matchedData);

        System.out.println("time taken = " + (System.currentTimeMillis() - l) + "ms");
    }

    private static void loadFromCosmosDB() {
        CosmosClient client = new CosmosClientBuilder()
                .endpoint(AccountSettings.HOST)
                .key(AccountSettings.MASTER_KEY)
                .preferredRegions(Collections.singletonList("East US 2"))
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .buildClient();

        CosmosDatabase database = client.getDatabase("my-database");
        CosmosDatabaseResponse read = database.read();
        System.out.println("read = " + read);

        CosmosContainer container = database.getContainer("UlineAddressBookPOC");
        CosmosContainerResponse read1 = container.read();
        System.out.println("read1 = " + read1);

        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        queryOptions.setQueryMetricsEnabled(true);


        CosmosPagedIterable<SiteBean> familiesPagedIterable = container.queryItems(
                "SELECT top 10 c.sitename,c.displayname,c.emailaddress FROM UlineAddressBookPOC c", queryOptions, SiteBean.class);
        for (SiteBean bean : familiesPagedIterable) {
            System.out.println("bean = " + bean);
            Map<String, String> personInfo = siteData.computeIfAbsent(bean.getSitename(), k -> new HashMap<>());
            personInfo.put(bean.getDisplayname(), bean.getEmailaddress());
        }
    }

    private static void loadData() throws IOException {
        long dataCount = 0;
        String directory = "C:\\Users\\admim\\Downloads\\site-data";
        String[] list = new File(directory).list();
        if (list != null) for (String fileName : list) {
            File file = new File(directory, fileName);
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;

                String[] split = line.split(",");
                String siteName = split[0].toLowerCase();
                String name = split[1];
                String email = split[2];
                Map<String, String> personInfo = siteData.computeIfAbsent(siteName, k -> new HashMap<>());
                personInfo.put(name, email);

                dataCount++;
            }
        }

        System.out.println("dataCount = " + dataCount);
    }
}