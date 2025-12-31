package group.msg.jpowermonitor.agent.export.csv;

import group.msg.jpowermonitor.agent.Unit;
import group.msg.jpowermonitor.agent.export.ResultsWriter;
import group.msg.jpowermonitor.dto.DataPoint;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import static group.msg.jpowermonitor.util.Constants.APP_TITLE;
import static group.msg.jpowermonitor.util.Constants.DATE_TIME_FORMATTER;
import static group.msg.jpowermonitor.util.Constants.DECIMAL_FORMAT;
import static group.msg.jpowermonitor.util.Constants.NEW_LINE;

/**
 * Write power and energy measurement results to CSV files at application shutdown.
 *
 * @author deinerj
 */
@Slf4j
@Getter
public class CsvResultsWriter implements ResultsWriter {
    protected static final String FILE_NAME_PREFIX = APP_TITLE + "_";

    private static final String dataPointFormatCsv;
    private static final String dataPointFormatEnergyConsumptionCsv;

    static {
        dataPointFormatCsv = Locale.getDefault().getCountry().toLowerCase(Locale.ROOT).equals("de") ? "%s;%s;%s;%s;%s;%s%s" : "%s,%s,%s,%s,%s,%s%s";
        dataPointFormatEnergyConsumptionCsv = Locale.getDefault().getCountry().toLowerCase(Locale.ROOT).equals("de") ? "%s;%s;%s;%s;%s;%s;%s;%s%s" : "%s,%s,%s,%s,%s,%s,%s,%s%s";
    }

    private final String energyConsumptionPerMethodFileName;
    private final String energyConsumptionPerFilteredMethodFileName;
    private final String powerConsumptionPerMethodFileName;
    private final String powerConsumptionPerFilteredMethodFileName;


    private final String resultsDirectoryOverride;

    /**
     * Constructor
     */
    public CsvResultsWriter() {
        long pid = ProcessHandle.current().pid();
        resultsDirectoryOverride = System.getProperty("jpowermonitor.csv.results.directory");
        this.energyConsumptionPerMethodFileName = FILE_NAME_PREFIX + pid + "_energy_per_method.csv";
        this.energyConsumptionPerFilteredMethodFileName = FILE_NAME_PREFIX + pid + "_energy_per_method_filtered.csv";
        this.powerConsumptionPerMethodFileName = FILE_NAME_PREFIX + pid + "_power_per_method.csv";
        this.powerConsumptionPerFilteredMethodFileName = FILE_NAME_PREFIX + pid + "_power_per_method_filtered.csv";
        log.debug("Energy consumption per method is written to '{}'", energyConsumptionPerMethodFileName);
        log.debug("Energy consumption per filtered methods is written to '{}'", energyConsumptionPerFilteredMethodFileName);
    }

    public void writeHeaders() {
        writeToFile("SystemTime,Time,ThreadName,Method,Value,Unit,CO2Value\n",resultsDirectoryOverride + "\\" + energyConsumptionPerFilteredMethodFileName, false);
        writeToFile("SystemTime,Time,ThreadName,Method,Value,Unit\n",resultsDirectoryOverride + "\\" + powerConsumptionPerFilteredMethodFileName, false);
        writeToFile("SystemTime,Time,ThreadName,Method,Value,Unit,CO2Value,CO2Unit\n",resultsDirectoryOverride + "\\" + energyConsumptionPerMethodFileName, false);
        writeToFile("SystemTime,Time,ThreadName,Method,Value,Unit\n",resultsDirectoryOverride + "\\" + powerConsumptionPerMethodFileName, false);
    }

    @Override
    public void writePowerConsumptionPerMethod(Map<String, DataPoint> measurements) {
        writeToFile(createCsv(measurements), resultsDirectoryOverride + "\\" + powerConsumptionPerMethodFileName, true);
    }

    @Override
    public void writePowerConsumptionPerMethodFiltered(Map<String, DataPoint> measurements) {
        writeToFile(createCsv(measurements), resultsDirectoryOverride + "\\" + powerConsumptionPerFilteredMethodFileName, true);
    }

    @Override
    public void writeEnergyConsumptionPerMethod(Map<String, DataPoint> measurements) {
        writeToFile(createCsv(measurements), resultsDirectoryOverride + "\\" + energyConsumptionPerMethodFileName, true);
    }

    @Override
    public void writeEnergyConsumptionPerMethodFiltered(Map<String, DataPoint> measurements) {
        writeToFile(createCsv(measurements), resultsDirectoryOverride + "\\" + energyConsumptionPerFilteredMethodFileName, true);
    }

    protected String createCsv(Map<String, DataPoint> measurements) {
        StringBuilder csv = new StringBuilder();
        measurements.forEach((method, energy) -> csv.append(createCsvEntryForDataPoint(energy)));
        return csv.toString();
    }

    protected String createCsvEntryForDataPoint(@NotNull DataPoint dp) {
        if (Unit.JOULE == dp.getUnit()) {
            return String.format(dataPointFormatEnergyConsumptionCsv,
                dp.getSystemTime(),
                DATE_TIME_FORMATTER.format(dp.getTime()),
                dp.getThreadName(),
                dp.getName(),
                DECIMAL_FORMAT.format(dp.getValue()),
                dp.getUnit(),
                DECIMAL_FORMAT.format(dp.getCo2Value()),
                Unit.GRAMS_CO2.getAbbreviation(),
                NEW_LINE);
        }
        return String.format(dataPointFormatCsv,
            dp.getSystemTime(),
            DATE_TIME_FORMATTER.format(dp.getTime()),
            dp.getThreadName(),
            dp.getName(),
            DECIMAL_FORMAT.format(dp.getValue()),
            dp.getUnit(),
            NEW_LINE);
    }

    protected void writeToFile(String csv, String fileName, boolean append) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, append))) {
            bw.write(csv);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}
