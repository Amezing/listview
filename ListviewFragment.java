
public class ListViewFragment extends Fragment{
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    
    
    /**
     * Returns a new instance of this fragment for the given section number.
     */
    public static ListViewFragment newInstance(int sectionNumber) {
      ListViewFragment fragment = new ListViewFragment();
      Bundle args = new Bundle();
      args.putInt(ARG_SECTION_NUMBER, sectionNumber);
      fragment.setArguments(args);
      return fragment;
    }

    public ListViewFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
      View rootView = inflater.inflate(R.layout.fragment_apps, container, false);
      Context context = getActivity();
      
      ListView apps = (ListView)rootView.findViewById(R.id.hot_apps);
      apps.setAdapter(new AppListAdapter(context));

      return rootView;
    }
    
    
    
    
    private class AppListAdapter extends BaseAdapter{
      private ArrayList<App> appList;
      private Context context;
      private int resLine = R.layout.list_app_item;
      
      public AppListAdapter(Context _context){
        context = _context;
        appList = App.list();
      }

      @Override
      public int getCount() {
        return appList.size();
      }

      @Override
      public App getItem(int position) {
        return appList.get(position);
      }

      @Override
      public long getItemId(int position) {
        return position;
      }
      
      //
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        //Android会复用被初始化的listview item，且并不固定
        Log.v("item "+ position + " null: ", "" + (convertView == null ? true : "from view "+((TextView)((ViewHolder)convertView.getTag()).getView(R.id.list_home_app_title)).getText()) );
        final App app = getItem(position);
        
        Log.v("app", app.name + (app.downloader == null ? " null" : " progress:" + app.downloader.getProgress()));
        ViewHolder holder = ViewHolder.newInstance(context, convertView, parent, resLine);
        
        Picasso.with(context).load(app.icon).placeholder(R.drawable.ic_launcher).into((ImageView)holder.getView(R.id.list_home_app_icon));
        ((TextView)holder.getView(R.id.list_home_app_title)).setText(app.name);
        
        final ProgressBar bar = holder.getView(R.id.list_home_app_progress);
        //问题：当bar不在当前窗口时，downloader仍然在更新bar，以至于复制过来的bar也在被更新，尽管bar.setProgress(0)
        //解决办法：区分downloader源，给bar标记一个当前downloader，来自其他downloader的请求时不更新
        bar.setTag(app.name);//用唯一的package name标记更好
        final Downloader.ProgressListener listener = new Downloader.ProgressListener() {
          public void onProgressChanged(final int progress) {
            if((String)bar.getTag() == app.name)bar.setProgress(progress);
          }
        };

        if(app.downloader == null){
          bar.setProgress(0);//clear progress
        }else{
          app.downloader.setListener(listener);
        }
        
        Button btn = holder.getView(R.id.list_home_app_download);
        btn.setOnClickListener(new OnClickListener(){
          @Override
          public void onClick(View v) {
            app.setDownloader(context);
            app.downloader.setListener(listener);
            Toast.makeText(context,"Start downloading "+app.downloadUrl, Toast.LENGTH_LONG).show();
            app.startDownload();
          }
        });
        
        return holder.convertView;
      }
    }
}


class App{
  public String icon;
  public String name;
  public double rating;
  public int downloads;
  public String downloadUrl;
  
  public Downloader downloader = null;
  
  public App(String _icon, String _name, double _rating, int _downloads, String _downloadUrl){
    icon = _icon;
    name = _name;
    rating = _rating;
    downloads = _downloads;
    downloadUrl = _downloadUrl;
  }
  
  
  public static ArrayList<App> list(){
    ArrayList<App> list = new ArrayList<App>();
    list.add(new App("http://a.creatist.cn/icon1.png", "微信", 4.5, 6200134, "http://a.creatist.cn/com.tencent.demoapp.apk"));
    list.add(new App("http://a.creatist.cn/icon2.png", "手机QQ", 4.3, 32004, "http://a.creatist.cn/avatar.jpg"));
    list.add(new App("http://a.creatist.cn/icon3.png", "手机QZone", 2.5, 35203, "http://a.creatist.cn/index.html"));
    list.add(new App("http://a.creatist.cn/icon4.png", "优酷", 4.5, 93200134, "http://a.creatist.cn/13.2M.pdf"));
    list.add(new App("http://a.creatist.cn/icon5.png", "暴风影音", 4.6, 41324, "http://a.creatist.cn/com.tencent.demoapp.apk"));
    list.add(new App("http://a.creatist.cn/icon6.png", "途牛", 4.8, 134, "http://a.creatist.cn/13.2M.pdf"));
    list.add(new App("http://a.creatist.cn/icon7.png", "YOYO", 4.9, 200134, "http://a.creatist.cn/com.tencent.demoapp.apk"));
    list.add(new App("http://a.creatist.cn/icon8.png", "淘宝", 4.0, 3220134, "http://a.creatist.cn/13.2M.pdf"));
    return list;
  }
  
  public void setDownloader(Context context){
    downloader = new Downloader(downloadUrl, name, context);
  }
  
  public boolean startDownload(){
    if(downloader == null)return false;
    else downloader.start();
    return true;
  }
}


class Downloader extends AsyncTask<String, Integer, String> {
  public final static int STATE_PENDING = 1;
  public final static int STATE_DOWNLOADING = 2;
  public final static int STATE_STOPED = 3;
  public final static int STATE_FINISHED = 4;
  
  private int state = STATE_PENDING;
  private PowerManager.WakeLock mWakeLock;
  private int progress = 0;
  
  private String url;
  private String appName;// for download rename
  private Context context;
  private Downloader.ProgressListener listener;
  
  public static interface ProgressListener{
    void onProgressChanged(final int progress);
  }
  
  public Downloader(String _url, String _appName, Context _context){
    url = _url;
    appName = _appName;
    context = _context;
  }
  
  public int getProgress(){
    return progress;
  }
  
  public void setListener(Downloader.ProgressListener _listener){
    listener = _listener;
    listener.onProgressChanged(progress);
  }
  
  public void start(){
    execute(url);
  }
  
  public int getState(){
    return state;
  }
  
  @Override
  protected String doInBackground(String... urls) {
    state = STATE_DOWNLOADING;
    InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urls[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
            }
            int fileLength = connection.getContentLength();

            input = connection.getInputStream();
            String path = Environment.getExternalStorageDirectory().getPath();
            output = new FileOutputStream(path + "/" + appName + ".apk");
            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                if (isCancelled()){
                  state = STATE_STOPED;
                  break;
                }
                total += count;
                // publishing the progress....
                if (fileLength > 0)publishProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (output != null)output.close();
                if (input != null)input.close();
            } catch (IOException ignored) {}
            if (connection != null)connection.disconnect();
        }
        return null;
  }
  
  @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // take CPU lock to prevent CPU from going off if the user 
        // presses the power button during download
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mWakeLock.acquire();
    }

    @Override
    protected void onProgressUpdate(Integer... progresses) {
        super.onProgressUpdate(progresses);
        progress = progresses[0];
        listener.onProgressChanged(progress);
        //Log.v("progress "+appName+":", progress[0].toString());
    }

    @Override
    protected void onPostExecute(String result) {
        mWakeLock.release();
        if (result != null){
          state = STATE_STOPED;
            Toast.makeText(context,"Download error: "+result, Toast.LENGTH_LONG).show();
        }else{
          state = STATE_FINISHED;
            Toast.makeText(context,"File downloaded", Toast.LENGTH_SHORT).show();
        }
    }
}

class ViewHolder{
  private SparseArray<View> holder;
  public View convertView;
  
  public ViewHolder(){
    holder = new SparseArray<View>();
  }
  
  public static ViewHolder newInstance(Context context, View convertView, ViewGroup parent, int layoutId){
    ViewHolder holder;
    if(convertView == null){
      holder = new ViewHolder();
      holder.convertView = LayoutInflater.from(context).inflate(layoutId, parent, false);
      holder.convertView.setTag(holder);
    }else{
      holder = (ViewHolder) convertView.getTag();
    }
    return holder;
  }
  
  public ViewHolder setView(int id, View view){
    holder.put(id, view);// id => view
    return this;
  }
  
  @SuppressWarnings("unchecked")
  public <T extends View> T getView(int viewId) {
        View view = holder.get(viewId);
        if (view == null) {
            view = convertView.findViewById(viewId);
            holder.put(viewId, view);
        }
        return (T) view;
    }
}
