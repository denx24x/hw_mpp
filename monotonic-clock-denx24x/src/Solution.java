import org.jetbrains.annotations.NotNull;

/**
 * В теле класса решения разрешено использовать только финальные переменные типа RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author Белозоров Денис
 */
public class Solution implements MonotonicClock {
    private final RegularInt c1_1 = new RegularInt(0);
    private final RegularInt c1_2 = new RegularInt(0);
    private final RegularInt c1_3 = new RegularInt(0);
    private final RegularInt c2_1 = new RegularInt(0);
    private final RegularInt c2_2 = new RegularInt(0);
    private final RegularInt c2_3 = new RegularInt(0);

    @Override
    public void write(@NotNull Time time) {
        c2_1.setValue(time.getD1());
        c2_2.setValue(time.getD2());
        c2_3.setValue(time.getD3());
        c1_3.setValue(time.getD3());
        c1_2.setValue(time.getD2());
        c1_1.setValue(time.getD1());
    }

    @NotNull
    @Override
    public Time read() {

        int[] r1 = new int[3];
        int[] r2 = new int[3];
        
        r1[0] = c1_1.getValue();
        r1[1] = c1_2.getValue();
        r1[2] = c1_3.getValue();
        r2[2] = c2_3.getValue();
        r2[1] = c2_2.getValue();
        r2[0] = c2_1.getValue();
    
        if(r1[0] == r2[0] && r1[1] == r2[1] && r1[2] == r2[2]){
            return new Time(r1[0], r1[1], r1[2]);
        }
        int p = 0;
        while(r1[p] == r2[p]){
            p += 1;
        }
        for(int i = p + 1; i < 3;i++){
            r2[i] = 0;
        }
        return new Time(r2[0], r2[1], r2[2]);
    }
}
