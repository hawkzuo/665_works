package your;

/**
 * Created by Amos on 2018/2/8.
 */
public class Interval {

     int start;
     int end;

    public Interval(int from, int to) {
        this.start = from;
        this.end = to;

    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Interval && start == ((Interval) obj).start && end == ((Interval) obj).end;
    }
}
