import java.util.*;
public final class Benchmark {
    private static Map<String,Object> request(String task, String text, String pattern) {
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("task",task); m.put("text",text); m.put("pattern",pattern); return m;
    }
    private static String median(long[] a) {
        long[] b=a.clone(); Arrays.sort(b); return Long.toString(b[b.length/2]);
    }
    public static void main(String[] args) {
        int[] ns={300,1000,3000};
        String[] tasks={"naive","kmp","rk"};
        System.out.println("case,n,m,algorithm,run1_ns,run2_ns,run3_ns,median_ns,correct");
        for (int n:ns) {
            int m=Math.max(1,n/10);
            for (String kind:new String[]{"random","repetitive"}) {
                String text, pattern;
                if (kind.equals("random")) {
                    Random random=new Random(20261107L+n);
                    StringBuilder t=new StringBuilder(), p=new StringBuilder();
                    for(int i=0;i<n;i++) t.append((char)('a'+random.nextInt(4)));
                    for(int i=0;i<m;i++) p.append((char)('a'+random.nextInt(4)));
                    text=t.toString(); pattern=p.toString();
                } else {
                    char[] t=new char[n], p=new char[m];
                    Arrays.fill(t,'a'); Arrays.fill(p,'a');
                    text=new String(t); pattern=new String(p);
                }
                String reference=Json.stringify(Algorithms.naive(request("naive",text,pattern)));
                for(String task:tasks) {
                    Map<String,Object> req=request(task,text,pattern);
                    String actual=Json.stringify(task.equals("naive")?Algorithms.naive(req):
                        task.equals("kmp")?Algorithms.kmp(req):Algorithms.rk(req));
                    if(!reference.equals(actual)) throw new AssertionError("incorrect output: "+kind+" "+n+" "+task);
                    for(int w=0;w<15;w++) {
                        if(task.equals("naive")) Algorithms.naive(req);
                        else if(task.equals("kmp")) Algorithms.kmp(req);
                        else Algorithms.rk(req);
                    }
                    long[] times=new long[3];
                    for(int r=0;r<3;r++) {
                        long start=System.nanoTime();
                        if(task.equals("naive")) Algorithms.naive(req);
                        else if(task.equals("kmp")) Algorithms.kmp(req);
                        else Algorithms.rk(req);
                        times[r]=System.nanoTime()-start;
                    }
                    System.out.printf(Locale.ROOT,"%s,%d,%d,%s,%d,%d,%d,%s,true%n",
                        kind,n,m,task,times[0],times[1],times[2],median(times));
                }
            }
        }
    }
}
