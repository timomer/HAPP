package com.hypodiabetic.happ.Graphs;

import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Stat;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.util.ChartUtils;

/**
 * Created by Tim on 16/02/2016.
 */
public class IOBCOBBarGraph extends CommonChartSupport{

    public IOBCOBBarGraph(Context context, Realm realm){super(context, realm); }

    private ColumnChartData columnData;
    private List<Stat> statList = Stat.updateActiveBarChart(context, realm);

    public ColumnChartData iobcobFutureChart() {

        if (!statList.isEmpty()) {

            List<Column> columnsData = new ArrayList<>();
            List<SubcolumnValue> values;
            List<AxisValue> xAxisValues = new ArrayList<AxisValue>();

            try {
                for (int v = 0; v < statList.size(); v++) {

                    values = new ArrayList<>();

                    //IOB
                    if (statList.get(v).getIob() > yIOBMax) {
                        values.add(new SubcolumnValue((float) (fitIOB2COBRange(yIOBMax.floatValue())), ChartUtils.COLOR_BLUE));
                    } else if (statList.get(v).getIob() < yIOBMin) {
                        values.add(new SubcolumnValue((float) (fitIOB2COBRange(yIOBMin.floatValue())), ChartUtils.COLOR_BLUE));
                    } else {
                        values.add(new SubcolumnValue((float) (fitIOB2COBRange(statList.get(v).getIob())), ChartUtils.COLOR_BLUE));
                    }
                    //COB
                    if (statList.get(v).getCob() > yCOBMax) {
                        values.add(new SubcolumnValue((float) (yCOBMax.floatValue()), ChartUtils.COLOR_ORANGE));
                    } else {
                        values.add(new SubcolumnValue((float) (statList.get(v).getCob()), ChartUtils.COLOR_ORANGE));
                    }

                    Column column = new Column(values);
                    column.setHasLabels(false);
                    columnsData.add(column);

                    AxisValue axisValue = new AxisValue(v);
                    axisValue.setLabel(statList.get(v).when);
                    xAxisValues.add(axisValue);
                    //xAxisValues.  add(new AxisValue((long)0, iobcobValues.getJSONObject(v).getString("when")));
                }
            } catch (Exception e) {
                Crashlytics.logException(e);
            }

            columnData = new ColumnChartData(columnsData);
            Axis axisX = new Axis(xAxisValues).setHasLines(true);

            //columnData.setAxisYLeft(ycobiobAxis());
            columnData.setAxisYLeft(iobPastyAxis());
            columnData.setAxisYRight(cobPastyAxis());
            columnData.setAxisXBottom(axisX);

            return columnData;

        } else{
            return new ColumnChartData(); //empty
        }
    }
}
