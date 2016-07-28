//Copyright (c) Microsoft Corporation All rights reserved.  
// 
//MIT License: 
// 
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
//documentation files (the  "Software"), to deal in the Software without restriction, including without limitation
//the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
//to permit persons to whom the Software is furnished to do so, subject to the following conditions: 
// 
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of
//the Software. 
// 
//THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
//CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.
package com.microsoft.band.sdk.sampleapp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandTheme;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.notifications.MessageFlags;
import com.microsoft.band.sdk.sampleapp.tileevent.R;
import com.microsoft.band.tiles.BandTile;
import com.microsoft.band.tiles.TileButtonEvent;
import com.microsoft.band.tiles.TileEvent;
import com.microsoft.band.tiles.pages.FilledButton;
import com.microsoft.band.tiles.pages.FilledButtonData;
import com.microsoft.band.tiles.pages.FlowPanel;
import com.microsoft.band.tiles.pages.FlowPanelOrientation;
import com.microsoft.band.tiles.pages.HorizontalAlignment;
import com.microsoft.band.tiles.pages.PageData;
import com.microsoft.band.tiles.pages.PageElementData;
import com.microsoft.band.tiles.pages.PageLayout;
import com.microsoft.band.tiles.pages.PageRect;
import com.microsoft.band.tiles.pages.TextButton;
import com.microsoft.band.tiles.pages.TextButtonData;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import com.microsoft.band.tiles.pages.ScrollFlowPanel;
import com.microsoft.band.tiles.pages.FlowPanelOrientation;
import com.microsoft.band.tiles.pages.PageData;
import com.microsoft.band.tiles.pages.PageLayout;
import com.microsoft.band.tiles.pages.TextBlock;
import com.microsoft.band.tiles.pages.TextBlockFont;
import com.microsoft.band.tiles.pages.ElementColorSource;
import com.microsoft.band.tiles.pages.VerticalAlignment;
import com.microsoft.band.tiles.pages.WrappedTextBlock;
import com.microsoft.band.tiles.pages.WrappedTextBlockData;
import com.microsoft.band.tiles.pages.WrappedTextBlockFont;

public class BandTileEventAppActivity extends Activity {

    private static int currentIndex = 0;
	private BandClient client = null;
	private Button btnStop;
	private Button btnStart;
	private TextView txtStatus;
	private ScrollView scrollView;

	private PageManager pageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStatus = (TextView) findViewById(R.id.txtStatus);
		scrollView = (ScrollView) findViewById(R.id.svTest);
        
		btnStart = (Button) findViewById(R.id.startButton);
        btnStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				disableButtons();
				new StartTask().execute();
			}
		});

		btnStop = (Button) findViewById(R.id.stopButton);
		btnStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				disableButtons();
				new StopTask().execute();
			}
		});

		pageManager = new PageManager(getBaseContext());

	}

	@Override
	protected void onNewIntent(Intent intent) {
    	processIntent(intent);
		super.onNewIntent(intent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if(getIntent() != null && getIntent().getExtras() != null){
			processIntent(getIntent());
		}
	}
	

    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }
    
    private void processIntent(Intent intent){
    	String extraString = intent.getStringExtra(getString(R.string.intent_key));
		
		if(extraString != null && extraString.equals(getString(R.string.intent_value))){
			if (intent.getAction() == TileEvent.ACTION_TILE_OPENED) {
	            TileEvent tileOpenData = intent.getParcelableExtra(TileEvent.TILE_EVENT_DATA);
                try {
                    pageManager.getConnectedBandClient();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BandException e) {
                    e.printStackTrace();
                }
                pageManager.createHomeView();
				appendToUI("Tile open event received\n" + tileOpenData.toString() + "\n\n");
			} else if (intent.getAction() == TileEvent.ACTION_TILE_BUTTON_PRESSED) {
				TileButtonEvent buttonData = intent.getParcelableExtra(TileEvent.TILE_EVENT_DATA);
                try {
                        if (buttonData.getElementID() == 99)
                        {
                            //Play Pressed
                            pageManager.createQuestion();
                        }
                        else
                        {
                            pageManager.OnAnswered(buttonData.getPageID(), buttonData.getElementID() == 21);

                        }
                    }
                    catch (BandException e){} catch (InterruptedException e) {
                }

                    appendToUI("Button 1 Pressed\n\n");
			} else if (intent.getAction() == TileEvent.ACTION_TILE_CLOSED) {
				TileEvent tileCloseData = intent.getParcelableExtra(TileEvent.TILE_EVENT_DATA);
				appendToUI("Tile close event received\n" + tileCloseData.toString() + "\n\n");
			}
		}
    }

    public class StartTask extends AsyncTask<Void, Void, Boolean> {
		@Override
	    protected void onPreExecute() {
			txtStatus.setText("");
	    }

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				if (pageManager.getConnectedBandClient()) {
					appendToUI("Band is connected.\n");
					if (pageManager.addTile(BandTileEventAppActivity.this)) {
					}
				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
					return false;
				}
			} catch (BandException e) {
				handleBandException(e);
				return false;
			} catch (Exception e) {
				appendToUI(e.getMessage());
				return false;
			}

			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				btnStop.setEnabled(true);
			} else {
				btnStart.setEnabled(true);
			}
		}
				}
				
	private class StopTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			appendToUI("Stopping demo and removing Band Tile\n");
			try {
				if (pageManager.getConnectedBandClient()) {
					appendToUI("Removing Tile.\n");
					pageManager.removeTile();
				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				handleBandException(e);
				return false;
			} catch (Exception e) {
				appendToUI(e.getMessage());
				return false;
			}
			
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				appendToUI("Stop completed.\n");
			}
			btnStart.setEnabled(true);
		}
			}

	private void disableButtons() {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				btnStart.setEnabled(false);
				btnStop.setEnabled(false);
			}
		});
	}
	
	private void appendToUI(final String string) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				txtStatus.append(string);
				scrollView.post(new Runnable() {
					@Override
					public void run() {
						scrollView.smoothScrollTo(0, txtStatus.getBottom());
					}

				});
			}
		});
	}


	private void handleBandException(BandException e) {
		String exceptionMessage = "";
		switch (e.getErrorType()) {
		case DEVICE_ERROR:
			exceptionMessage = "Please make sure bluetooth is on and the band is in range.\n";
			break;
		case UNSUPPORTED_SDK_VERSION_ERROR:
			exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
			break;
		case SERVICE_ERROR:
			exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
			break;
		case BAND_FULL_ERROR:
			exceptionMessage = "Band is full. Please use Microsoft Health to remove a tile.\n";
			break;
		default:
			exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
			break;
		}
		appendToUI(exceptionMessage);
	}
}
