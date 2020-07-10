package com.chatapp.adapters;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.core.EventDisplay;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomState;
import org.matrix.androidsdk.data.RoomSummary;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.RoomMember;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import im.vector.Matrix;
import im.vector.R;
import im.vector.VectorApp;
import im.vector.activity.MXCActionBarActivity;
import im.vector.activity.VectorRoomActivity;
import im.vector.util.VectorUtils;
import im.vector.view.VectorCircularImageView;

public class RoomListAdapter extends RecyclerView.Adapter<RoomListAdapter.ViewHolder> {


    private List<RoomSummary> roomList;
    Context context;


    private View view1;
    private ViewHolder viewHolder1;
    private int lastPosition = 0;
    private RoomSummary summary;

    private MXSession session;


    public RoomListAdapter(Context context, List<RoomSummary> roomslist) {
        this.context = context;
        session = Matrix.getInstance(context).getDefaultSession();
        session.getDataHandler().addListener(mLiveEventListener);
        this.roomList = roomslist;


    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        view1 = LayoutInflater.from(context).inflate(R.layout.adapter_room_list, parent, false);
        viewHolder1 = new ViewHolder(view1);

        return viewHolder1;
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {


        summary = roomList.get(position);
        Room room = session.getDataHandler().getStore().getRoom(summary.getRoomId());
        if (TextUtils.isEmpty(room.getAvatarUrl())) {
           holder.tvRoomProfile.setImageResource(R.drawable.default_contact_avatar);
        } else {
            VectorUtils.loadRoomAvatar(context, session, holder.tvRoomProfile, room);
        }

        if (roomList.get(position).getUnreadEventsCount() == 0) {
            holder.tvUreadCount.setVisibility(View.GONE);
        } else {
            holder.tvUreadCount.setVisibility(View.VISIBLE);
            holder.tvUreadCount.setText(roomList.get(position).getUnreadEventsCount() + "");
        }

        holder.tvRoomName.setText(summary.getLatestRoomState().name);
        holder.tvRoomName.setText(room.getRoomDisplayName(context));
        holder.card_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO
                /*
                Intent intent = new Intent(context, MessageActivity.class);
                intent.putExtra(MessageActivity.EXTRA_ROOM_ID, roomList.get(position).getRoomId());
                context.startActivity(intent);
                 */
                Intent intent = new Intent(context, VectorRoomActivity.class);
                intent.putExtra(VectorRoomActivity.EXTRA_ROOM_ID, roomList.get(position).getRoomId());
                intent.putExtra(MXCActionBarActivity.EXTRA_MATRIX_ID, session.getMyUserId());
                context.startActivity(intent);
            }
        });

        RoomState latestRoomState = summary.getLatestRoomState();

        EventDisplay display = new EventDisplay(context);
        display.setPrependMessagesWithAuthor(true);
        String message = display.getTextualDisplay(summary.getLatestReceivedEvent(), latestRoomState) + "";
        String mLatestTypingMessage = null;
        List<String> typingUsers = room.getTypingUsers();

        if ((null != typingUsers) && (typingUsers.size() > 0)) {
            String myUserId = session.getMyUserId();


            // get the room member names
            ArrayList<String> names = new ArrayList<>();

            for (int i = 0; i < typingUsers.size(); i++) {
                RoomMember member = room.getMember(typingUsers.get(i));

                // check if the user is known and not oneself
                if ((null != member) && !TextUtils.equals(myUserId, member.getUserId()) && (null != member.displayname)) {
                    names.add(member.displayname);
                }
            }

            Locale locale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locale = context.getResources().getConfiguration().getLocales().get(0);
            } else {
                locale = context.getResources().getConfiguration().locale;
            }

            // nothing to display ?
            if (0 == names.size()) {
                mLatestTypingMessage = null;
            } else if (1 == names.size()) {
                mLatestTypingMessage = String.format(locale, context.getString(R.string.room_one_user_is_typing), names.get(0));
            } else if (2 == names.size()) {
                mLatestTypingMessage = String.format(locale, context.getString(R.string.room_two_users_are_typing), names.get(0), names.get(1));
            } else if (names.size() > 2) {
                mLatestTypingMessage = String.format(locale, context.getString(R.string.room_many_users_are_typing), names.get(0), names.get(1));
            }
        }

        if (mLatestTypingMessage != null) {
            holder.tvRoomMessage.setTextColor(context.getResources().getColor(R.color.green_800));
            holder.tvRoomMessage.setText(mLatestTypingMessage);
        } else {
            holder.tvRoomMessage.setText(message);
        }


        String timestamp = getFormattedTimestamp(summary.getLatestReceivedEvent());
        String currentDateAndTime = getFormattedDate(summary.getLatestReceivedEvent().getOriginServerTs()).toUpperCase();
        if (currentDateAndTime.equalsIgnoreCase("today")) {
            holder.tvTime.setText(convertDate(summary.getLatestReceivedEvent().getOriginServerTs() + "", "hh:mm a"));

        } else {
            holder.tvTime.setText(currentDateAndTime);
        }


    }

    public static String convertDate(String dateInMilliseconds, String dateFormat) {
        return DateFormat.format(dateFormat, Long.parseLong(dateInMilliseconds)).toString();
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tvRoomProfile)
        ImageView tvRoomProfile;
        @BindView(R.id.tvRoomName)
        TextView tvRoomName;
        @BindView(R.id.tvUreadCount)
        TextView tvUreadCount;
        @BindView(R.id.tvRoomMessage)
        TextView tvRoomMessage;
        @BindView(R.id.tvTime)
        TextView tvTime;
        @BindView(R.id.card_view)
        CardView card_view;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    protected String getFormattedTimestamp(Event event) {
        return event.formattedOriginServerTs();
    }

    MXEventListener mLiveEventListener = new MXEventListener() {
        @Override
        public void onLiveEvent(Event event, RoomState roomState) {

            if (event.type.equals(Event.EVENT_TYPE_TYPING)) {
                notifyDataSetChanged();
            }
        }

    };

    public String getFormattedDate(long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMilis);

        Calendar now = Calendar.getInstance();

        final String dateTimeFormatString = "dd/MM/yyyy";
        if (now.get(Calendar.DATE) == smsTime.get(Calendar.DATE)) {
            return "Today";
        } else if (now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1) {
            return "Yesterday";
        } else if (now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)) {
            return DateFormat.format(dateTimeFormatString, smsTime).toString();
        } else {
            return DateFormat.format(dateTimeFormatString, smsTime).toString();
        }
    }

}