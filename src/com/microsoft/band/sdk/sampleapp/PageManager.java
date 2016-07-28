package com.microsoft.band.sdk.sampleapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sdk.sampleapp.tileevent.R;
import com.microsoft.band.sensors.BandPedometerEvent;
import com.microsoft.band.sensors.BandPedometerEventListener;
import com.microsoft.band.tiles.BandTile;
import com.microsoft.band.tiles.TileButtonEvent;
import com.microsoft.band.tiles.pages.FlowPanel;
import com.microsoft.band.tiles.pages.FlowPanelOrientation;
import com.microsoft.band.tiles.pages.HorizontalAlignment;
import com.microsoft.band.tiles.pages.Icon;
import com.microsoft.band.tiles.pages.IconData;
import com.microsoft.band.tiles.pages.PageData;
import com.microsoft.band.tiles.pages.PageLayout;
import com.microsoft.band.tiles.pages.ScrollFlowPanel;
import com.microsoft.band.tiles.pages.TextButton;
import com.microsoft.band.tiles.pages.TextButtonData;
import com.microsoft.band.tiles.pages.VerticalAlignment;
import com.microsoft.band.tiles.pages.WrappedTextBlock;
import com.microsoft.band.tiles.pages.WrappedTextBlockData;
import com.microsoft.band.tiles.pages.WrappedTextBlockFont;

import java.util.List;
import java.util.UUID;

/**
 * Created by ssebast on 7/27/2016.
 */
public class PageManager{
    private static final UUID tileId = UUID.fromString("cc0D508F-70A3-47D4-BBA3-812BADB1F8Aa");
    private static final UUID homePageId = UUID.fromString("b1234567-89ab-cdef-0123-456789abcd03");
    private static final UUID questionPageId = UUID.fromString("b1234567-89ab-cdef-0123-456789abcd00");

    private int optionLeftId = 1;
    private int optionRightId = 2;
    private int skipButtonId = 3;
    private int playButtonId = 4;
    private int homeStatusId = 5;
    private int questionTextId = 6;
    private int homeMessageId= 7;
    private int healthIconIdStart = 100; // Do Not add 100+health for any other element

    private long firstStep = -1;

    private static final int MAX_HEALTH = 2;
    private static int health = MAX_HEALTH;

    private BandClient client = null;
    private Question q;

    static int currentIndex = 0;

    Context context;
    public PageManager(Context context)
    {
        this.context = context;
    }

    public String GetHomeMessage()
    {
        if (health <=0 ) return "Run 500 mts for fuel-up";

        return "";
    }

    public void createHomeView() {
        try {
            client.getTileManager().removePages(tileId);

            PageData data = new PageData(homePageId, 0)
                    .update(new WrappedTextBlockData(homeStatusId, "Lives: " + health))
                    .update(new WrappedTextBlockData(homeMessageId, GetHomeMessage()))
                    .update(new TextButtonData(playButtonId, "Play"));

            for (int i = 0; i < health; i++)
            {
                data.update(new IconData(healthIconIdStart+i,0));
            }
            client.getTileManager().setPages(tileId, data);

        } catch (BandIOException e) {
            e.printStackTrace();
        }
    }

    public void removeTile() throws BandIOException, InterruptedException, BandException {
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

    public boolean addTile(BandTileEventAppActivity activity) throws Exception {
        if (doesTileExist()) {
            return true;
        }
        Question.generateQuestions(context);

        client.getSensorManager().registerPedometerEventListener(mPedometerEventListener);
		/* Set the options */
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap tileIcon = BitmapFactory.decodeResource(context.getResources(), R.raw.b_icon, options);

        BandTile tile = new BandTile.Builder(tileId, "Button Tile", tileIcon)
                .setPageLayouts(CreateHomeLayout(), CreateQuestionLayout())
                .setPageIcons(tileIcon)
                .build();
        //appendToUI("Button Tile is adding ...\n");
        if (client.getTileManager().addTile(activity, tile).await()) {
            //appendToUI("Button Tile is added.\n");
            return true;
        } else {
            //appendToUI("Unable to add button tile to the band.\n");
            return false;
        }
    }

    public void createQuestion()
    {
        try {
            if (health <= 0)
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

    private PageLayout CreateHomeLayout()
    {
        TextButton button1 = new TextButton(0, 5, 100, 45).setMargins(0, 5, 0, 0).setId(playButtonId).setPressedColor(Color.YELLOW);

        FlowPanel healthIndicator = new FlowPanel(0, 0, 245, 30, FlowPanelOrientation.HORIZONTAL);
        for (int i = 0; i < MAX_HEALTH; i++)
        {
            Icon icon = new Icon(0, 0, 25, 25).setId(healthIconIdStart + i);
            healthIndicator.addElements(icon);
        }

        return new PageLayout(
                new ScrollFlowPanel(0, 0, 245, 100, FlowPanelOrientation.VERTICAL).setHorizontalAlignment(HorizontalAlignment.CENTER).setVerticalAlignment(VerticalAlignment.TOP)
                        .addElements(
                                healthIndicator,
                                new WrappedTextBlock(0, 0, 245, 202, WrappedTextBlockFont.SMALL).setId(homeStatusId).setColor(Color.GREEN).setAutoHeightEnabled(true),
                                new WrappedTextBlock(0, 0, 245, 202, WrappedTextBlockFont.SMALL).setId(homeMessageId).setColor(Color.YELLOW).setAutoHeightEnabled(true),
                                new FlowPanel(0,0,100,45,FlowPanelOrientation.HORIZONTAL).addElements(button1).setHorizontalAlignment(HorizontalAlignment.CENTER)));
    }

    private PageLayout CreateQuestionLayout() {
        TextButton button1 = new TextButton(0, 5, 245, 45).setMargins(0, 15, 0, 0).setId(optionLeftId).setPressedColor(Color.GRAY).setHorizontalAlignment(HorizontalAlignment.CENTER);
        TextButton button2 = new TextButton(0, 0, 245, 45).setMargins(0, 15, 0, 0).setId(optionRightId).setPressedColor(Color.GRAY).setHorizontalAlignment(HorizontalAlignment.CENTER);

        TextButton skipButton = new TextButton(0, 0, 100, 45).setMargins(115, 15, 0, 0).setId(skipButtonId).setPressedColor(Color.GRAY).setHorizontalAlignment(HorizontalAlignment.CENTER);

        return new PageLayout(
                new ScrollFlowPanel(0, 0, 245, 100, FlowPanelOrientation.VERTICAL).setHorizontalAlignment(HorizontalAlignment.LEFT).setVerticalAlignment(VerticalAlignment.TOP)
                        .addElements(
                                new WrappedTextBlock(0, 0, 245, 202, WrappedTextBlockFont.SMALL).setId(questionTextId).setColor(Color.WHITE).setAutoHeightEnabled(true),
                                new FlowPanel(0,0,245,120,FlowPanelOrientation.VERTICAL).addElements(button1, button2).setHorizontalAlignment(HorizontalAlignment.CENTER),
                                skipButton));
    }

    public void updatePages() throws BandIOException {
        q = Question.getAQuestion();
        String[] options = q.getOptions();
        client.getTileManager().setPages(tileId,
                new PageData(questionPageId, 1)
                        .update(new WrappedTextBlockData(questionTextId, q.getQuestionTitle()))
                        .update(new TextButtonData(optionLeftId, options[0]))
                        .update(new TextButtonData(optionRightId, options[1]))
                        .update(new TextButtonData(skipButtonId, "Next")));
    }

    private void UpdateHealth(int newValue)
    {
        if (newValue < 0 || newValue > MAX_HEALTH)
            return;
        health = newValue;
    }

    public void OnAnswered(UUID pageId, Boolean isCorrect) throws BandException, InterruptedException {
        String message = (isCorrect?"Correct!!":"Wrong :(");
        message += "\n" + q.getMessage();
        String[] options = q.getOptions();

        client.getTileManager().setPages(tileId,
                new PageData(pageId, 1)
                        .update(new WrappedTextBlockData(questionTextId, message))
                        .update(new TextButtonData(optionLeftId, options[0]))
                        .update(new TextButtonData(optionRightId, options[1]))
                        .update(new TextButtonData(skipButtonId, "Next")));

        if (isCorrect)
        {
            UpdateHealth(health+1);
        }
        else
        {
            UpdateHealth(health-1);
        }
    }

    public void OnButtonClicked(TileButtonEvent buttonData)
    {
        if (buttonData.getElementID() == playButtonId)
        {
            //Play Pressed
            createQuestion();
        }
        else if (buttonData.getElementID() == skipButtonId)
        {
            UpdateHealth(health-1);
            createQuestion();
        }
        else
        {
            try {
                OnAnswered(buttonData.getPageID(), q.checkAnswer(buttonData.getElementID() == optionLeftId ? 0 : 1));
            } catch (BandException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
    private BandPedometerEventListener mPedometerEventListener = new BandPedometerEventListener() {
        @Override
        public void onBandPedometerChanged(final BandPedometerEvent event) {
            if (event != null) {
                if (firstStep < 0) {
                    firstStep = event.getTotalSteps();
                    Log.d("Pedometer", String.valueOf(event.getTotalSteps()));
                }
                if (event.getTotalSteps() - firstStep > 5) {
                    firstStep = -1;
                    UnregisterPedometer();
                    UpdateHealth(2);

                }

            }
        }
    };

    public void UnregisterPedometer() {
        if (client != null) {
            try {
                client.getSensorManager().unregisterPedometerEventListener(mPedometerEventListener);
            } catch (BandIOException e) {
                //appendToUI(e.getMessage());
            }
        }
    }

    public boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                //appendToUI("Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(context, devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        //appendToUI("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }

}
