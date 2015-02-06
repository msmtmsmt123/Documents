package com.alexandrepiveteau.filemanager.ui;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.alexandrepiveteau.filemanager.R;
import com.alexandrepiveteau.filemanager.adapters.FileListAdapter;
import com.alexandrepiveteau.filemanager.adapters.listeners.OnDataObjectClickListener;
import com.alexandrepiveteau.filemanager.adapters.listeners.OnDataObjectInfoClickListener;
import com.alexandrepiveteau.filemanager.adapters.listeners.OnDataObjectLongClickListener;
import com.alexandrepiveteau.filemanager.dialogs.DataObjectActionDialog;
import com.alexandrepiveteau.filemanager.dialogs.DataObjectInfoDialog;
import com.alexandrepiveteau.filemanager.dialogs.listeners.OnDataObjectActionListener;
import com.alexandrepiveteau.filemanager.files.DataObject;
import com.alexandrepiveteau.filemanager.files.DataObjectAction;
import com.alexandrepiveteau.filemanager.managers.ActionBarManager;
import com.alexandrepiveteau.filemanager.ui.listeners.OnDataActionPerformedListener;
import com.alexandrepiveteau.filemanager.utils.DataObjectActionsUtils;
import com.alexandrepiveteau.filemanager.utils.DataObjectOpenerUtils;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements
        View.OnClickListener,
        OnDataObjectActionListener,
        OnDataObjectClickListener,
        OnDataObjectInfoClickListener,
        OnDataObjectLongClickListener,
        OnDataActionPerformedListener {

    private static final String KEY_DIRECTORY = "KEY_DIRECTORY";

    private DataObject.ACTION mActionPending = DataObject.ACTION.NONE;
    private boolean isActionPending;
    private List<DataObject> mItemsSelected;

    private File dir;
    private List<DataObject> mFiles;
    private List<DataObject> mFolders;

    private FloatingActionButton mContextualFloatingActionButton;

    private ActionBarDrawerToggle mActionBarDrawerToggle;
    private ActionBarManager mActionBarManager;
    private DrawerLayout mDrawerLayout;
    private FileListAdapter mFilesAdapter;
    private FloatingActionButton mFloatingActionButton;
    private LinearLayoutManager mLinearLayoutManager;
    private RecyclerView mListView;
    private TextView mTitle;
    private Toolbar mToolbar;

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.fab:
                File oldDir = dir;
                try {
                    dir = dir.getParentFile();
                    setDocumentsList(dir);
                } catch (Exception e) {
                    dir = oldDir;
                }
                break;
            case R.id.contextual_fab:
                if(isActionPending) {
                    switch (mActionPending) {
                        case CUT:
                            List<File> mCutFiles = new ArrayList<File>();
                            for(DataObject object : mItemsSelected) {
                                mCutFiles.add(new File(dir, object.getName()));
                            }
                            DataObjectActionsUtils.cutDataObjects(this, mItemsSelected, mCutFiles, this);
                            mActionPending = DataObject.ACTION.NONE;
                            isActionPending = false;
                            mActionBarManager.setFloatingActionButtonVisibility(true);
                            mActionBarManager.setContextualFloatingActionButtonVisibility(false);
                            mActionBarManager.resetContextualActionButtonImage();
                            break;
                        case COPY:
                            List<File> mCopyFiles = new ArrayList<File>();
                            for(DataObject object : mItemsSelected) {
                                mCopyFiles.add(new File(dir, object.getName()));
                            }
                            DataObjectActionsUtils.copyDataObjects(this, mItemsSelected, mCopyFiles, this);
                            mActionPending = DataObject.ACTION.NONE;
                            isActionPending = false;
                            mActionBarManager.setFloatingActionButtonVisibility(true);
                            mActionBarManager.setContextualFloatingActionButtonVisibility(false);
                            mActionBarManager.resetContextualActionButtonImage();
                            break;
                    }
                }
                else {
                    DataObjectActionDialog dataObjectActionDialog = new DataObjectActionDialog(this);
                    dataObjectActionDialog.setDataObjectActionListener(this);
                    dataObjectActionDialog.show();
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dir = Environment.getExternalStorageDirectory();

        mContextualFloatingActionButton = (FloatingActionButton) findViewById(R.id.contextual_fab);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mListView = (RecyclerView) findViewById(R.id.files_list);
        mTitle = (TextView) findViewById(R.id.toolbar_title);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        mLinearLayoutManager = new LinearLayoutManager(this);
        mListView.setLayoutManager(mLinearLayoutManager);
        mListView.setHasFixedSize(true);

        mActionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close);
        mActionBarManager = new ActionBarManager(this,mContextualFloatingActionButton , mFloatingActionButton, mTitle);

        isActionPending = false;
        mItemsSelected = new ArrayList<DataObject>();
        mContextualFloatingActionButton.hide(false);
        mContextualFloatingActionButton.setOnClickListener(this);
        mFloatingActionButton.setOnClickListener(this);
        mActionBarManager.setFile(dir);
        mDrawerLayout.setDrawerListener(mActionBarDrawerToggle);
        setActionBar(mToolbar);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        setDocumentsList(dir);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mActionBarDrawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onDataActionCancelled() {
        mItemsSelected.clear();
        Toast.makeText(this, R.string.action_cancelled, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDataActionPerformed() {
        mItemsSelected.clear();
        setDocumentsList(dir);
    }

    @Override
    public void onDataObjectActionClick(DataObjectAction action) {
        boolean showContFab = false;
        boolean showFab = false;
        switch (action.getAction()) {
            case CUT:
                isActionPending = true;
                mActionPending = DataObject.ACTION.CUT;
                mActionBarManager.setContextualActionButtonImage(R.drawable.ic_action_paste);
                showFab = true;
                showContFab = true;
                break;
            case COPY:
                isActionPending = true;
                mActionPending = DataObject.ACTION.COPY;
                mActionBarManager.setContextualActionButtonImage(R.drawable.ic_action_paste);
                showFab = true;
                showContFab = true;
                break;
            case DELETE:
                showFab = true;
                showContFab = false;
                DataObjectActionsUtils.deleteDataObjects(this, mItemsSelected, this);
                break;
            case RENAME:
                showFab = true;
                showContFab = false;
                DataObjectActionsUtils.renameDataObject(this, mItemsSelected, this);
                break;
        }
        mActionBarManager.setContextualFloatingActionButtonVisibility(showContFab);
        mActionBarManager.setFloatingActionButtonVisibility(showFab);
        //setDocumentsList(dir);
    }

    @Override
    public void onDataItemClick(DataObject dataObject) {
        File oldDir = dir;
        try {
            dir = dataObject.getFile();
            setDocumentsList(dir);
        } catch (Exception e) {
            dir = DataObjectOpenerUtils.openFile(oldDir, dir, this);
        }
    }

    @Override
    public void onDataItemInfoClick(DataObject dataObject) {
        DataObjectInfoDialog dialog = new DataObjectInfoDialog(this, dataObject);
        dialog.show();
    }

    @Override
    public void onDataItemLongClick(DataObject dataObject, int position) {
        mFilesAdapter.setItemClicked(position);

        boolean addObject = true;

        for (DataObject obj : mItemsSelected) {
            if (obj.getFile().getPath().equals(dataObject.getFile().getPath())) {
                mItemsSelected.remove(obj);
                addObject = false;
                break;
            } else {
                addObject = true;
            }
        }

        if(addObject) {
            mItemsSelected.add(dataObject);
        }

        for (boolean b : mFilesAdapter.mItemsSelected) {
            if (b) {
                mActionBarManager.setContextualFloatingActionButtonVisibility(true);
                mActionBarManager.setFloatingActionButtonVisibility(false);
                return;
            } else {
                mActionBarManager.setContextualFloatingActionButtonVisibility(false);
                mActionBarManager.setFloatingActionButtonVisibility(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mActionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch(item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mActionBarDrawerToggle.syncState();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        dir = new File(savedInstanceState.getString(KEY_DIRECTORY, Environment.getExternalStorageDirectory().getPath()));
        setDocumentsList(dir);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_DIRECTORY, dir.getPath());
    }

    private void setDocumentsList(File file) {
        mFolders = DataObject.getDirectoriesFrom(this, file);
        mFiles = DataObject.getFilesFrom(this, file);
        mFilesAdapter = new FileListAdapter(this, mFolders, mFiles, this, this, this);
        mListView.setAdapter(mFilesAdapter);
        mActionBarManager.setFile(dir);
        if(!isActionPending) {
            mItemsSelected.clear();
            mActionBarManager.setContextualFloatingActionButtonVisibility(false);
            mActionBarManager.setFloatingActionButtonVisibility(true);
        }
    }
}