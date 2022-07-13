package ${YYAndroidPackageName};

import android.os.AsyncTask;
import android.util.Log;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Thread;
import com.yoyogames.runner.RunnerJNILib;


enum DownloadStatus
{
    /// <summary></summary>
	NotConnected,
	Connecting,
	Connected,
	Downloading,
	Complete,
	Error,
	Canceled,
	SettingsChanged,
}

 public class RunnerDownloadTask extends AsyncTask<String, Integer, Boolean> {
 
 	public DownloadStatus Status;
 	public int Progress;
 	private ProgressDialog progressDialog;
 	
 	
 	private void setStatus( DownloadStatus _status, String _msg )
 	{
 		Status = _status;
 		String msg = "";
 		switch( _status ) {
 		case Connecting:
	 		msg = "Connecting to " + _msg + ".....";
	 		break;
	 	case Connected:
	 		msg = "Connected to " + _msg + ", activating polarity flow...";
	 		break;
	 	case Error:
	 		msg = "Error - " + _msg + ", retrying...";
	 		break;
	 	case Complete:
	 		msg = "Complete...";
	 		break;
 		} // end switch
 		
 		RunnerActivity.DownloadTaskStatus = _status;
 		final String f = msg;
		RunnerActivity.ViewHandler.post( new Runnable() {
    		public void run() {
    			progressDialog.setMessage( f );
    		}
    	 });

 	} // end setStatus
 	
 	protected void onPreExecute() {
 		Status = DownloadStatus.NotConnected;
 		progressDialog = new ProgressDialog( RunnerJNILib.ms_context );
 		progressDialog.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
 		progressDialog.setMessage( "Activating phase inducer...." );
		progressDialog.setTitle("v" + RunnerActivity.m_versionName);
 		progressDialog.setCancelable( false );
 		progressDialog.setButton("Change Settings", new DialogInterface.OnClickListener() {
 			public void onClick(DialogInterface dialog, int which) {
				Intent settingsActivity = new Intent( RunnerJNILib.ms_context, RunnerPreferenceActivity.class );
				RunnerJNILib.ms_context.startActivity( settingsActivity );
				Status = DownloadStatus.SettingsChanged;
 			} // end onClick
 		});
 		progressDialog.show();
 	}
 	
     protected Boolean doInBackground(String... urls) {
         int count = urls.length;
    	 boolean ret = false;
         for (int i = 0; i < count; i+=2) {
         	while( Status != DownloadStatus.Complete ) {
         	
         		// wait for a bit so we see the error message
         		if (Status == DownloadStatus.Error ) {
         			try {
         			Thread.sleep( 1000 );
         			} catch( Exception _e ) {
         				// do nothing...
         			} // end catch
         		} // end if
         	
				try {
					Log.i( "yoyo", "DownloadFileTo( " + urls[i] + " , " + urls[i+1] + " )" );
					URL url = new URL( urls[i] );
					HttpURLConnection connection = (HttpURLConnection)url.openConnection();
					connection.setDoInput( true );
					//connection.setDoOutput( true );
					connection.setUseCaches( false );
					Log.i( "yoyo", "about to connect to " + urls[i] );
					setStatus( DownloadStatus.Connecting, url.getHost() );
					connection.connect();
					int responseCode = connection.getResponseCode();
					if (responseCode == 200 ) {
					
						Log.i( "yoyo", "connected to " + urls[i] );
						setStatus( DownloadStatus.Connected, url.getHost() );
						//File fileOutput = new File( urls[i+1] );
						//Log.i( "yoyo", "creating new file " + urls[i+1] );
						//fileOutput.createNewFile();
						Log.i( "yoyo", "opening file " + urls[i+1] );
						FileOutputStream fs = new FileOutputStream( urls[i+1] );
						Log.i( "yoyo", "getting the input stream " );
						InputStream in = connection.getInputStream();
						byte[] buffer = new byte[8192];
						int len1 = 0;
						int curr = 0;
						Log.i( "yoyo", "get content length " );
						int totalSize = connection.getContentLength();
						Log.i( "yoyo", "starting loop " );
						while( (len1 = in.read(buffer)) > 0) {
							Status = DownloadStatus.Downloading;
							Log.i( "yoyo", "write buffer " );
							fs.write( buffer, 0, len1 );
							Log.i( "yoyo", "written buffer " );
							curr += len1;
							int percentage = (curr*100)/totalSize;
							if (percentage > 100) percentage = 100;
							if (percentage < 0) percentage = 0;
							Log.i( "yoyo", "downloaded " + len1 + " bytes - " + percentage + "% complete");
							publishProgress( percentage );
							Log.i( "yoyo", "looping " );
						} // end while
						fs.close();
						setStatus( DownloadStatus.Complete, "" );
						ret = true;
					} // end if
					else {
						Log.i( "yoyo", "response Code " + responseCode );
					} // end else
				} // end try
				catch( MalformedURLException _e) {
					Log.i( "yoyo", "Exception on DownloadFileTo " + _e );
					setStatus( DownloadStatus.Error, "malformed URL" );
				} // end catch
				catch( ProtocolException _e) {
					Log.i( "yoyo", "Exception on DownloadFileTo " + _e );
					setStatus( DownloadStatus.Error, "protocol error" );
				} // end catch
				catch( FileNotFoundException _e) {
					Log.i( "yoyo", "Exception on DownloadFileTo " + _e );
					setStatus( DownloadStatus.Error, "file not found" );
				} // end catch
				catch( IOException _e) {
					Log.i( "yoyo", "Exception on DownloadFileTo " + _e );
					setStatus( DownloadStatus.Error, "io error" );
				} // end catch
			} // end while
         } // end for
         return ret;
     }

     protected void onProgressUpdate(Integer... progress) {
     	progressDialog.setProgress( progress[0] );
         //setProgressPercent(progress[0]);
     }

     protected void onPostExecute(Boolean result) {
     	progressDialog.dismiss();
     }
 }