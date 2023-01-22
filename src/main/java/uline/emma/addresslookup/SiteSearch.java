package uline.emma.addresslookup;

import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RestController
public class SiteSearch {
    private static final Map<String, Map<String, String>> siteData = new HashMap<>();

    public static void main(String[] args) {
        SpringApplication.run(SiteSearch.class, args);
    }

    @GetMapping(name = "/")
    public String hello() {
        return "address lookup";
    }

    @GetMapping(name = "/search")
    public static void search(@RequestParam String siteName, @RequestParam String displayName) {
        long l = System.currentTimeMillis();

        Map<String, String> matchedData = new HashMap<>();

        Map<String, String> personInfo = siteData.get(siteName);
        if (personInfo != null) for (Map.Entry<String, String> entry : personInfo.entrySet()) {
            String personName = entry.getKey();
            String email = entry.getValue();

            if (personName.toLowerCase().contains(displayName) || email.toLowerCase().contains(displayName))
                matchedData.put(personName, email);
        }

        System.out.println("matchedData = " + matchedData);

        System.out.println("time taken = " + (System.currentTimeMillis() - l) + "ms");
    }

    @GetMapping("/load")
    public static void loadFromCosmosDB() {
        try (CosmosClient client = new CosmosClientBuilder()
                .endpoint(AccountSettings.HOST)
                .key(AccountSettings.MASTER_KEY)
                .preferredRegions(Collections.singletonList("East US 2"))
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .buildClient()) {

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
    }
}