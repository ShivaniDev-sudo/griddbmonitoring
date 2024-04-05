package mycode.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toshiba.mwcloud.gs.*;

import mycode.dto.CpuMetric;

@Service
public class MetricsCollectionService {
  @Autowired
  GridStore store;

  @Scheduled(fixedRate = 6000) // Collect metrics every minute
  public void collectMetrics() throws GSException {
    // Fetch CPU usage metrics from Spring Boot Actuator endpoint
    double cpuUsage = getCPUUsageFromActuator();
    Date timestamp = new Date();

    // Create a CPU metric object
    CpuMetric cpuMetric = new CpuMetric(timestamp, cpuUsage);

    // Store the metric in GridDB
    System.out.println("Fetching CPU metrics at current time");
    TimeSeries<CpuMetric> ts = store.putTimeSeries("cpuMetrics", CpuMetric.class);
    ts.append(cpuMetric);
  }

  private double getCPUUsageFromActuator() {
    String actuatorUrl = "http://localhost:8080/actuator/metrics/system.cpu.usage";
    RestTemplate restTemplate = new RestTemplate();
    try {
      ResponseEntity<String> responseEntity = restTemplate.getForEntity(actuatorUrl, String.class);

      if (responseEntity.getStatusCode() == HttpStatus.OK) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(responseEntity.getBody());
        JsonNode measurements = root.path("measurements");

        if (measurements.isArray() && measurements.size() > 0) {
          JsonNode valueNode = measurements.get(0).path("value");
          if (valueNode.isDouble())
            return valueNode.asDouble();
        }
      }
    } catch (HttpClientErrorException e) {
      // Handle HTTP client errors
      System.err.println("HTTP error: " + e.getMessage());
    } catch (Exception e) {
      // Handle other exceptions
      System.err.println("Error: " + e.getMessage());
    }

    // Return a default value if unable to fetch or parse the CPU usage
    return -1.0;
  }
}
