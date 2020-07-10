package com.chatapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chatapp.Customprogress.CustomProgressDialog;
import com.chatapp.FusionPBXRoomCreationActivity;
import com.chatapp.RoomCreationActivity;
import com.chatapp.adapters.RoomListAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.callback.SimpleApiCallback;
import org.matrix.androidsdk.core.model.MatrixError;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomState;
import org.matrix.androidsdk.data.RoomSummary;
import org.matrix.androidsdk.data.store.IMXStore;
import org.matrix.androidsdk.listeners.IMXEventListener;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.rest.model.Event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import im.vector.Matrix;
import im.vector.R;

public class ChatListFragment extends Fragment {
    View vChatListFragment;
    @BindView(R.id.rvChatList)
    RecyclerView rvChatList;
    Unbinder unbinder;
    @BindView(R.id.tvEmpty)
    TextView tvEmpty;
    @BindView(R.id.fabchat)
    FloatingActionButton fabChat;
    private RoomListAdapter roomListAdapter;
    private HashMap<MXSession, IMXEventListener> mListeners;
    public static HomeServerConnectionConfig hsConfig;
    public IMXStore store;
    public static MXSession session;
    private Context mContext;
    private CustomProgressDialog customProgressDialog;
    private ArrayList<RoomSummary> mMainRoomList = new ArrayList<>();

    public ChatListFragment() {
        // Required empty public constructor
    }

    public static ChatListFragment newInstance() {
        ChatListFragment fragment = new ChatListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        vChatListFragment = inflater.inflate(R.layout.activity_chat_list, container, false);
        unbinder = ButterKnife.bind(this, vChatListFragment);
        customProgressDialog = new CustomProgressDialog(mContext);
        mListeners = new HashMap<MXSession, IMXEventListener>();
        fabChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), FusionPBXRoomCreationActivity.class);
                startActivity(intent);
            }
        });

        rvChatList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE)
                {
                    fabChat.show();
                }
                super.onScrollStateChanged(recyclerView, newState);

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 ||dy<0 && fabChat.isShown())
                {
                    fabChat.hide();
                }
            }
        });

        return vChatListFragment;
    }


    private void callAdapter() {

        customProgressDialog.show();
        Matrix.getInstance(mContext).getDefaultSession().getDataHandler().addListener(mLiveEventListener);
        Collection<Room> rooms = Matrix.getInstance(mContext).getDefaultSession().getDataHandler().getStore().getRooms();
        for (Room room : rooms) {
            if (room.isInvited()) {
                room.join(null);
            }
        }
        mMainRoomList.clear();
        mMainRoomList.addAll(Matrix.getInstance(mContext).getDefaultSession().getDataHandler().getStore().getSummaries());
        sortSummaries();
        LinearLayoutManager recylerViewLayoutManager = new LinearLayoutManager(mContext);
        rvChatList.setLayoutManager(recylerViewLayoutManager);
        roomListAdapter = new RoomListAdapter(mContext, mMainRoomList);
        rvChatList.setAdapter(roomListAdapter);
        customProgressDialog.dismiss();
        callEmptyTextDisplay();

    }

    private void callEmptyTextDisplay() {
        if (mMainRoomList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvChatList.setVisibility(View.GONE);
        } else {
            if (tvEmpty != null) {
                tvEmpty.setVisibility(View.GONE);
                rvChatList.setVisibility(View.VISIBLE);
            }
        }

    }

    public void sortSummaries() {
        for (int section = 0; section < mMainRoomList.size(); ++section) {
            Collections.sort(mMainRoomList, new Comparator<RoomSummary>() {
                public int compare(RoomSummary lhs, RoomSummary rhs) {
                    return lhs != null && lhs.getLatestReceivedEvent() != null ? (rhs != null && rhs.getLatestReceivedEvent() != null ? (
                            lhs.getLatestReceivedEvent().getOriginServerTs() > rhs.getLatestReceivedEvent().getOriginServerTs() ? -1 : (lhs.getLatestReceivedEvent().getOriginServerTs() < rhs.getLatestReceivedEvent().getOriginServerTs() ? 1 : 0)) : -1) : 1;
                }
            });
        }

    }

    MXEventListener mLiveEventListener = new MXEventListener() {
        @Override
        public void onLiveEvent(Event event, RoomState roomState) {


            if (event.type.equals(Event.EVENT_TYPE_MESSAGE)) {


                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mMainRoomList.clear();
                        mMainRoomList.addAll(Matrix.getInstance(mContext).getSessions().get(0).getDataHandler().getStore().getSummaries());
                        sortSummaries();
                        roomListAdapter.notifyDataSetChanged();

                        callEmptyTextDisplay();
                    }
                }, 1500);
            }

            if (event.type.equals(Event.EVENT_TYPE_STATE_ROOM_MEMBER)) {
                Room room = Matrix.getInstance(mContext).getDefaultSession().getDataHandler().getStore().getRoom(roomState.roomId);

                if (room.isInvited())
                    room.join(new SimpleApiCallback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mMainRoomList.clear();
                            mMainRoomList.addAll(Matrix.getInstance(mContext).getSessions().get(0).getDataHandler().getStore().getSummaries());
                            sortSummaries();

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    callEmptyTextDisplay();
                                    roomListAdapter.notifyDataSetChanged();
                                }
                            });
                        }

                        @Override
                        public void onNetworkError(Exception e) {

                        }

                        @Override
                        public void onMatrixError(MatrixError matrixError) {

                        }

                        @Override
                        public void onUnexpectedError(Exception e) {

                        }
                    });
            }
        }

    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
try {
    Matrix.getInstance(mContext).getDefaultSession().getDataHandler().removeListener(mLiveEventListener);
}catch (Exception e){}
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;

    }

    @Override
    public void onResume() {
        super.onResume();
        callAdapter();
    }

}
