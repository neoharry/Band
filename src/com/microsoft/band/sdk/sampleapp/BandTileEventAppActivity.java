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
import android.app.Fragment;
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

    private static final UUID tileId = UUID.fromString("cc0D508F-70A3-47D4-BBA3-812BADB1F8Aa");
    private static final UUID homePageId = UUID.fromString("b1234567-89ab-cdef-0123-456789abcd03");
    private static final UUID questionPageId = UUID.fromString("b1234567-89ab-cdef-0123-456789abcd00");

    private static int currentIndex = 0;
    private static int health = 2;
	private BandClient client = null;
	private Button btnStop;
	private Button btnStart;
	private TextView txtStatus;
	private ScrollView scrollView;

    private Question q;

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
                    getConnectedBandClient();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BandException e) {
                    e.printStackTrace();
                }
                createHomeView();
				appendToUI("Tile open event received\n" + tileOpenData.toString() + "\n\n");
			} else if (intent.getAction() == TileEvent.ACTION_TILE_BUTTON_PRESSED) {
				TileButtonEvent buttonData = intent.getParcelableExtra(TileEvent.TILE_EVENT_DATA);
                try {
                        if (buttonData.getElementID() == 99)
                        {
                            //Play Pressed
                            createQuestion();
                        }
                        else
                        {
                            OnAnswered(buttonData.getPageID(), buttonData.getElementID() == 21);

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

    private void createHomeView() {
        try {
            client.getTileManager().removePages(tileId);
            client.getTileManager().setPages(tileId,
                    new PageData(homePageId, 0)
                            .update(new WrappedTextBlockData(1, "Lives: " + health))
                            .update(new TextButtonData(99, "Play")));
        } catch (BandIOException e) {
            e.printStackTrace();
        }
    }

    private class StartTask extends AsyncTask<Void, Void, Boolean> {
		@Override
	    protected void onPreExecute() {
			txtStatus.setText("");
	    }

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				if (getConnectedBandClient()) {
					appendToUI("Band is connected.\n");
					if (addTile()) {
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
				if (getConnectedBandClient()) {
					appendToUI("Removing Tile.\n");
					removeTile();
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

	private void removeTile() throws BandIOException, InterruptedException, BandException {
		if (doesTileExist()) {
			client.getTileManager().removeTile(tileId).await();
		}
	}
	

	private boolean doesTileExist() throws BandIOException, InterruptedException, BandException {
		List<BandTile> tiles = client.getTileManager().getTiles().await();
		for (BandTile tile : tiles) {
			if (tile.getTileId().equals(tileId)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean addTile() throws Exception {
		if (doesTileExist()) {
			return true;
		}
		
		/* Set the options */
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap tileIcon = BitmapFactory.decodeResource(getBaseContext().getResources(), R.raw.b_icon, options);

        BandTile tile = new BandTile.Builder(tileId, "Button Tile", tileIcon)
			.setPageLayouts(createOneButtonLayout(), createButtonLayout())
			.build();
		appendToUI("Button Tile is adding ...\n");
		if (client.getTileManager().addTile(this, tile).await()) {
			appendToUI("Button Tile is added.\n");
			return true;
		} else {
			appendToUI("Unable to add button tile to the band.\n");
			return false;
		}
	}

    private void createQuestion()
    {
        Question.generateQuestions(getBaseContext());
        try {
            if (currentIndex >= 3 || currentIndex < 0)
            {
                //Purge all pages if any
                createHomeView();
                return ;
            }

            updatePages();
        }
        catch(BandIOException e)
        {}
        catch(BandException e)
        {}
    }

    private PageLayout createOneButtonLayout()
    {
        TextButton button1 = new TextButton(0, 5, 100, 45).setMargins(0, 5, 0, 0).setId(99).setPressedColor(Color.YELLOW);

        return new PageLayout(
                new ScrollFlowPanel(0, 0, 245, 100, FlowPanelOrientation.VERTICAL).setHorizontalAlignment(HorizontalAlignment.CENTER).setVerticalAlignment(VerticalAlignment.TOP)
                        .addElements(
                                new WrappedTextBlock(0, 0, 245, 202, WrappedTextBlockFont.SMALL).setId(1).setColor(Color.CYAN).setAutoHeightEnabled(true),
                                new TextBlock(0,0,245, 50, TextBlockFont.SMALL).setId(2).setColor(Color.YELLOW).setAutoWidthEnabled(true),
                                new FlowPanel(0,0,100,45,FlowPanelOrientation.HORIZONTAL).addElements(button1).setHorizontalAlignment(HorizontalAlignment.CENTER)));
    }

	private PageLayout createButtonLayout() {
        TextButton button1 = new TextButton(0, 5, 100, 45).setMargins(0, 5, 0, 0).setId(12).setPressedColor(Color.GRAY);
        TextButton button2 = new TextButton(0, 0, 100, 45).setMargins(5, 5, 0, 0).setId(21).setPressedColor(Color.GRAY);
		return new PageLayout(
				new ScrollFlowPanel(0, 0, 245, 100, FlowPanelOrientation.VERTICAL).setHorizontalAlignment(HorizontalAlignment.LEFT).setVerticalAlignment(VerticalAlignment.TOP)
					.addElements(
                            new WrappedTextBlock(0, 0, 245, 202, WrappedTextBlockFont.SMALL).setId(1).setColor(Color.WHITE).setAutoHeightEnabled(true),
                            new FlowPanel(0,0,220,100,FlowPanelOrientation.HORIZONTAL).addElements(button1).addElements(button2)));
	}
	
	private void updatePages() throws BandIOException {
		q = Question.getAQuestion();
		String[] options = q.getOptions();
        client.getTileManager().setPages(tileId,
                new PageData(questionPageId, 1)
                        .update(new WrappedTextBlockData(1, q.getQuestionTitle()))
                        .update(new TextButtonData(12, options[0]))
                        .update(new TextButtonData(21, options[1])));
	}

    private void OnAnswered(UUID pageId, Boolean isCorrect) throws BandException, InterruptedException {
        String message = (isCorrect?"Correct!!":"Wrong :(");
        message += "\n" + q.getMessage();
        String[] options = q.getOptions();

        client.getTileManager().setPages(tileId,
                new PageData(pageId, 1)
                        .update(new WrappedTextBlockData(1, message))
                        .update(new TextButtonData(12, options[0]))
                        .update(new TextButtonData(21, options[1])));

        if (isCorrect)
        {
            health++;
            if (health > 2) health = 2;
        }
        else
        {
            health--;
            if (health < 1) return ;
        }
        currentIndex++;

        Thread.sleep(3000);
        createQuestion();
    }

	private boolean getConnectedBandClient() throws InterruptedException, BandException {
		if (client == null) {
			BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
			if (devices.length == 0) {
				appendToUI("Band isn't paired with your phone.\n");
				return false;
			}
			client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
		} else if (ConnectionState.CONNECTED == client.getConnectionState()) {
			return true;
		}
		
		appendToUI("Band is connecting...\n");
		return ConnectionState.CONNECTED == client.connect().await();
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
