package org.acme;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

import com.google.gson.GsonBuilder;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.timeseries.Forecast;
import ai.djl.timeseries.SampleForecast;
import ai.djl.timeseries.TimeSeriesData;
import ai.djl.timeseries.dataset.FieldName;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.DeferredTranslatorFactory;
import ai.djl.translate.TranslateException;
import eu.hansolo.fx.charts.Axis;
import eu.hansolo.fx.charts.ChartType;
import eu.hansolo.fx.charts.Grid;
import eu.hansolo.fx.charts.XYPane;
import eu.hansolo.fx.charts.data.XYChartItem;
import eu.hansolo.fx.charts.series.XYSeries;
import eu.hansolo.fx.charts.series.XYSeriesBuilder;
import eu.hansolo.fx.charts.tools.Helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

    private static final Double AXIS_WIDTH = 25d;
 
     private eu.hansolo.fx.charts.XYChart<XYChartItem> multiTimeSeriesChart;
     private Axis xAxis;
     private Axis yAxis;

    @Override public void init() {
         // Data Series 1
         List<XYSeries<XYChartItem>>    listOfSeries1  = new ArrayList<>();
         String                         filename1      = App.class.getResource("data1.csv").toExternalForm().replaceAll("file:", "");
         String                         data1          = Helper.readTextFile(filename1);
         String[]                       lines1         = data1.split(System.getProperty("line.separator"));
         String                         firstLine1     = lines1[0];
         String[]                       names1         = firstLine1.split(",");
         List<Double>                   xAxisValues1   = new ArrayList<>();
         double                         yAxisMinValue1 = Double.MAX_VALUE;
         double                         yAxisMaxValue1 = Double.MIN_VALUE;
         Map<String, List<XYChartItem>> seriesDataMap1 = new HashMap<>();
         for (int i = 1 ; i < lines1.length ; i++) {
             String line = lines1[i];
             List<XYChartItem> xyItems = new ArrayList<>();
             String[] dataPoints = line.split(",");
             double   timePoint  = Double.parseDouble(dataPoints[0]);
             xAxisValues1.add(timePoint);
             for (int j = 1 ; j < dataPoints.length ; j++) {
                 double value = Double.parseDouble(dataPoints[j]);
                 yAxisMinValue1 = Math.min(yAxisMinValue1, value);
                 yAxisMaxValue1 = Math.max(yAxisMaxValue1, value);
 
                 if (seriesDataMap1.containsKey(names1[j])) {
                     seriesDataMap1.get(names1[j]).add(new XYChartItem(timePoint, value, names1[j], Color.MAGENTA));
                 } else {
                     seriesDataMap1.put(names1[j], new LinkedList<>());
                     seriesDataMap1.get(names1[j]).add(new XYChartItem(timePoint, value, names1[j], Color.MAGENTA));
                 }
             }
         }
 
         seriesDataMap1.entrySet().forEach(entry -> {
             XYSeries<XYChartItem> xySeries = XYSeriesBuilder.create()
                                                             .items(entry.getValue().toArray(new XYChartItem[0]))
                                                             .chartType(ChartType.MULTI_TIME_SERIES)
                                                             .fill(Color.TRANSPARENT)
                                                             .stroke(Color.MAGENTA)
                                                             .symbolFill(Color.RED)
                                                             .symbolStroke(Color.TRANSPARENT)
                                                             .symbolsVisible(false)
                                                             .symbolSize(5)
                                                             .strokeWidth(0.5)
                                                             .build();
             listOfSeries1.add(xySeries);
         });
 
         double yAxisMinValue = yAxisMinValue1;
         double yAxisMaxValue = yAxisMaxValue1;
 
 
         // MultiTimeSeriesChart
         double start = xAxisValues1.stream().min(Comparator.comparingDouble(Double::doubleValue)).get();
         double end   = xAxisValues1.stream().max(Comparator.comparingDouble(Double::doubleValue)).get();
         xAxis = Helper.createBottomAxis(start, end, "Time [s]", true, AXIS_WIDTH);
         xAxis.setDecimals(1);
 
         yAxis = Helper.createLeftAxis(yAxisMinValue, yAxisMaxValue, "Ratio", true, AXIS_WIDTH);
         yAxis.setDecimals(2);
 
         xAxis.setZeroColor(Color.BLACK);
         yAxis.setZeroColor(Color.BLACK);
 
         XYPane xyPane1 = new XYPane(listOfSeries1);
         xyPane1.setAverageStroke(Color.rgb(247, 118, 109));
         xyPane1.setAverageStrokeWidth(3);
         xyPane1.setStdDeviationFill(Color.rgb(247, 118, 109, 0.2));
         xyPane1.setStdDeviationStroke(Color.rgb(120, 120, 120));
         xyPane1.setEnvelopeVisible(false);
         xyPane1.setEnvelopeFill(Color.TRANSPARENT);
         xyPane1.setEnvelopeStroke(Color.rgb(247, 118, 109));
 
         List<XYPane> xyPanes = new ArrayList<>();
         xyPanes.add(xyPane1);
 
         multiTimeSeriesChart = new eu.hansolo.fx.charts.XYChart(xyPanes, yAxis, xAxis);
 
         Grid grid1 = new Grid(xAxis, yAxis);
         multiTimeSeriesChart.setGrid(grid1);
     }

     @Override public void start(Stage stage) {
         StackPane pane = new StackPane(multiTimeSeriesChart);
         pane.setPadding(new Insets(10));
 
         Scene scene = new Scene(new StackPane(pane), 800, 600);
 
         stage.setTitle("MultiTimeSeriesCharts");
         stage.setScene(scene);
         stage.show();
     }

    /*@Override
    public void start(Stage stage) {

        List<Calculation> predictions = new ArrayList<>();
        try {
            predictions.addAll(predict());
            System.out.println(predictions.get(0));
            System.out.println(predictions.get(1));
        } catch (ModelNotFoundException | MalformedModelException | IOException | TranslateException e) {
            e.printStackTrace();
        }

        stage.setTitle("Air Passengers forecast");
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Month/Year");
        final LineChart<String,Number> lineChart = 
                new LineChart<String,Number>(xAxis,yAxis);
       
                // https://ts.gluon.ai/v0.11.x/index.html#simple-example
                //https://d2kv9n23y3w0pn.cloudfront.net/static/README/forecasts.png

        lineChart.setTitle("Air Passengers Past and Prediction");
                          
        XYChart.Series series1 = generateChart("Past", 
                                                predictions.get(0).calculation, 
                                                YearMonth.parse("1949-01"));

        XYChart.Series series2 = generateChart("Prediction", 
                                                predictions.get(1).calculation, 
                                                YearMonth.parse("1961-01"));

        Scene scene  = new Scene(lineChart,2048,600);       
        lineChart.getData().addAll(series1, series2);

        stage.setScene(scene);
        stage.show();
    }

    private XYChart.Series generateChart(String title, float[] values, YearMonth initial) {

        XYChart.Series series = new XYChart.Series();
        series.setName(title);
        YearMonth currentDate = initial;
        for (float value : values) {
            series.getData().add(new XYChart.Data(currentDate.toString(), value));
            currentDate = currentDate.plusMonths(1);
        }

        return series;
    }*/

    private List<Calculation> predict() throws MalformedURLException, IOException, ModelNotFoundException, MalformedModelException, TranslateException {

        List<Calculation> calculations = new ArrayList<>();

        Criteria<TimeSeriesData, Forecast> criteria =
                Criteria.builder()
                        .setTypes(TimeSeriesData.class, Forecast.class)
                        .optModelUrls("djl://ai.djl.mxnet/deepar/0.0.1/airpassengers")
                        .optEngine("MXNet")
                        .optTranslatorFactory(new DeferredTranslatorFactory())
                        .optArgument("prediction_length", 12)
                        .optArgument("freq", "M")
                        .optArgument("use_feat_dynamic_real", false)
                        .optArgument("use_feat_static_cat", false)
                        .optArgument("use_feat_static_real", false)
                        .optProgress(new ProgressBar())
                        .build();
        
                        // 144 elements
        String url = "https://resources.djl.ai/test-models/mxnet/timeseries/air_passengers.json";

        try (ZooModel<TimeSeriesData, Forecast> model = criteria.loadModel();
                Predictor<TimeSeriesData, Forecast> predictor = model.newPredictor();
                NDManager manager = NDManager.newBaseManager("MXNet")) {
            TimeSeriesData input = getTimeSeriesData(manager, URI.create(url).toURL());

            // save data for plotting
            NDArray target = input.get(FieldName.TARGET);
            target.setName("target");
            
            calculations.add(new Calculation("target", target));

            Forecast forecast = predictor.predict(input);

            // save data for plotting. Please see the corresponding python script from
            // https://gist.github.com/Carkham/a5162c9298bc51fec648a458a3437008
            NDArray samples = ((SampleForecast) forecast).getSortedSamples();
            samples.setName("samples");
            
            NDArray prediction = forecast.mean();
            calculations.add(new Calculation("prediction", prediction));

            return calculations;
        }
    }

    private static TimeSeriesData getTimeSeriesData(NDManager manager, URL url) throws IOException {
        try (Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
            AirPassengers passengers =
                    new GsonBuilder()
                            .setDateFormat("yyyy-MM")
                            .create()
                            .fromJson(reader, AirPassengers.class);

            LocalDateTime start =
                    passengers.start.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            NDArray target = manager.create(passengers.target);
            TimeSeriesData data = new TimeSeriesData(10);
            data.setStartTime(start);
            data.setField(FieldName.TARGET, target);
            return data;
        }
    }


    public static void main(String[] args) {
        launch();
    }

    private static class Calculation {

        Calculation(String name, NDArray calculation) {
            this.name = name;
            float[] aux = calculation.toFloatArray();
            this.calculation = Arrays.copyOf(aux, aux.length);
        }

        float[] calculation;
        String name;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("==== ").append(name).append(" =====").append(System.lineSeparator());

            sb.append(Arrays.toString(calculation)).append(System.lineSeparator());
            
            sb.append("================================").append(System.lineSeparator());
            return sb.toString();
        }
    }

    private static final class AirPassengers {

        Date start;
        float[] target;
    }

}