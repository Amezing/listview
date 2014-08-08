# Updating progress bar in ListView

Sample code to demonstrate the idea, worked, but not tested.

    public View getView(int position, View convertView, ViewGroup parent) {
        final App app = getItem(position);
        
        ViewHolder holder = ViewHolder.newInstance(context, convertView, parent, resLine);
        
        Picasso.with(context).load(app.icon).placeholder(R.drawable.ic_launcher)
          .into((ImageView)holder.getView(R.id.list_home_app_icon));
        ((TextView)holder.getView(R.id.list_home_app_title)).setText(app.name);
        
        final ProgressBar bar = holder.getView(R.id.list_home_app_progress);
        bar.setTag(app.name);
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