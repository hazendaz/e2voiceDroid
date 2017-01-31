
package com.example.zhoupeng.e2voicedroid;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

public class MessagesListAdapter extends BaseAdapter {
    private Context context;
    private int count;
    private static LayoutInflater layoutInflater;

    public MessagesListAdapter(Context context) {
        this.context = context;
        count = Conversation.getCount();
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;

        TextMessage item = Conversation.getMessage(position);

        if (convertView == null) {
//            if ("video".equals(item.getFrom())) {
//                convertView = layoutInflater.inflate(R.layout.message_response_video, null);
//                holder = new Holder();
//                holder.data = (TextView) convertView.findViewById(R.id.rx_msg_video_rx_text);
//                holder.addtionData = (TextView)convertView.findViewById(R.id.rx_msg_video_title_text);
//                holder.videoButton = (ImageButton) convertView.findViewById(R.id.rx_msg_video_button);
//            }
//            else
            if ("moreInfo".equals(item.getFrom())) {
                convertView = layoutInflater.inflate(R.layout.message_response_moreinfo, null);
                holder = new Holder();
                holder.data = (TextView) convertView.findViewById(R.id.rx_msg_moreinfor_rx_text);
                holder.addtionData = (TextView)convertView.findViewById(R.id.rx_msg_addl_text);
            }
            else if ("sys".equals(item.getFrom())) {
                convertView = layoutInflater.inflate(R.layout.message_system, null);
                holder = new Holder();
                holder.data = (TextView) convertView.findViewById(R.id.editTextUserDetailInput_sys);
            } else if ("tx".equals(item.getFrom())) {
                convertView = layoutInflater.inflate(R.layout.message_sent, null);
                holder = new Holder();
                holder.data = (TextView) convertView.findViewById(R.id.editTextUserDetailInput_tx);
            } else {
                convertView = layoutInflater.inflate(R.layout.message_response, null);
                holder = new Holder();
                holder.data = (TextView) convertView.findViewById(R.id.editTextUserDetailInput_rx);
//                holder.addtionData = (TextView)convertView.findViewById(R.id.rx_msg_addl_text);
            }
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.data.setText(item.getMessage());
        ResponseCard rc = item.getResponseCard();
        if(rc != null && holder.addtionData != null)
        {
            if(rc.getAdditionMsg() != null)
            {
                holder.addtionData.setText(rc.getAdditionMsg());
            }

            if(rc.getVideoUrl()!=null)
            {
                Drawable img = context.getResources().getDrawable( R.drawable.video_icon_image64 );
                holder.addtionData.setCompoundDrawablesWithIntrinsicBounds(null, null, img, null);
            }
            else if(rc.getWebUrl()!=null)
            {
                Drawable img = context.getResources().getDrawable( R.drawable.web_site );
                if(rc.getWebUrl().contains("youtube.com"))
                    img = context.getResources().getDrawable( R.drawable.youtube_icon );
                holder.addtionData.setCompoundDrawablesWithIntrinsicBounds(null, null, img, null);
            }
            else if(rc.getOptions()!=null)
            {
                if(rc.getAdditionMsg() != null)
                {
                    String msg = rc.getAdditionMsg();
                    for(int i=0; i< rc.getOptions().size();i++)
                    {
                        msg += ("<p><i>"+rc.getOptions().get(i) + "</i></p>");
                    }
                    holder.addtionData.setText(Html.fromHtml(msg));
                }
            }
        }
        return convertView;
    }

    // Helper class to recycle View's
    static class Holder {
        TextView data;
        TextView addtionData;
        ImageButton videoButton;
    }

    // Add new items
    public void refreshList(TextMessage message) {
        Conversation.add(message);
        notifyDataSetChanged();
    }

}
