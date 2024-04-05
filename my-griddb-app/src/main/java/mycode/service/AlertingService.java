package mycode.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.toshiba.mwcloud.gs.Container;
import com.toshiba.mwcloud.gs.GridStore;
import com.toshiba.mwcloud.gs.Query;
import com.toshiba.mwcloud.gs.Row;
import com.toshiba.mwcloud.gs.RowSet;

import mycode.dto.CpuMetric;

@Service
public class AlertingService {

  private final double cpuUsageThreshold = 90.0; // CPU usage threshold in percentage
  @Autowired
  GridStore store;

  @Scheduled(fixedRate = 60000) // Check metrics every minute
  public void monitorAndAlert() throws Exception {
    // Fetch the latest CPU metric from GridDB

    double currentThreshold = calculateDynamicThreshold();
    if (currentThreshold > cpuUsageThreshold) {
      // CPU usage exceeds threshold, trigger alert
      // sendAlert(currentThreshold);
    }
  }

  // public void sendAlert(double cpuUsage) {
  // Email from = new Email("your@example.com");
  // String subject = "CPU Alert - High CPU Usage Detected";
  // Email to = new Email("admin@example.com");
  // Content content = new Content("text/plain",
  // "CPU usage has exceeded the threshold." + cpuUsage + "at" + Instant.now());
  // Mail mail = new Mail(from, subject, to, content);

  // SendGrid sg = new SendGrid(sendGridApiKey);
  // Request request = new Request();
  // try {
  // request.setMethod(Method.POST);
  // request.setEndpoint("mail/send");
  // request.setBody(mail.build());
  // Response response = sg.api(request);
  // System.out.println(response.getStatusCode());
  // System.out.println(response.getBody());
  // System.out.println(response.getHeaders());
  // } catch (IOException ex) {
  // ex.printStackTrace();
  // }
  // }

  private double calculateDynamicThreshold() throws Exception {

    Container<?, Row> container = store.getContainer("cpuMetrics");
    if (container == null) {
      throw new Exception("Container not found.");
    }

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    Date now = new Date();
    Date sixHoursAgo = new Date(now.getTime() - TimeUnit.HOURS.toMillis(6));

    String nowString = dateFormat.format(now);
    String sixHoursAgoString = dateFormat.format(sixHoursAgo);

    String queryString = "select * where timestamp >= TIMESTAMP('" + sixHoursAgoString
        + "') and timestamp <= TIMESTAMP('" + nowString + "')";
    System.out.println(queryString);
    Query<Row> query = container.query(queryString);
    // Fetch the results using RowSet
    RowSet<Row> rs = query.fetch();

    // Process the fetched CPU metrics
    double totalCpuUsage = 0.0;
    int count = 0;
    while (rs.hasNext()) {
      Row row = rs.next();
      Double cpuUsage = row.getDouble(1);
      totalCpuUsage += cpuUsage;
      count++;
      System.out
          .println("Timestamp: " + row.getTimestamp(0) + ", CPU Usage: " + row.getDouble(1));
    }

    // Calculate the average CPU usage over the past six hours
    double averageCpuUsage = (count > 0) ? (totalCpuUsage / count) : 0.0;

    // Perform additional calculations based on averageCpuUsage and return the
    // result
    double dynamicThreshold = calculateThreshold(averageCpuUsage);

    return dynamicThreshold;
  }

  private double calculateThreshold(double averageCpuUsage) {
    return averageCpuUsage * 1.2;
  }

}
