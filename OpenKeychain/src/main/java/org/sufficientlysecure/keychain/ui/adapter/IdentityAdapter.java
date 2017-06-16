/*
 * Copyright (C) 2017 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui.adapter;


import java.util.List;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.linked.UriAttribute;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.ui.adapter.IdentityAdapter.ViewHolder;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityLoader.IdentityInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityLoader.LinkedIdInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityLoader.UserIdInfo;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;
import org.sufficientlysecure.keychain.ui.util.SubtleAttentionSeeker;


public class IdentityAdapter extends RecyclerView.Adapter<ViewHolder> {
    private static final int VIEW_TYPE_USER_ID = 0;
    private static final int VIEW_TYPE_LINKED_ID = 1;


    private final Context context;
    private final LayoutInflater layoutInflater;
    private final boolean isSecret;

    private List<IdentityInfo> data;


    public IdentityAdapter(Context context, boolean isSecret) {
        super();
        this.layoutInflater = LayoutInflater.from(context);
        this.context = context;
        this.isSecret = isSecret;
    }

    public void setData(List<IdentityInfo> data) {
        this.data = data;

        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        IdentityInfo info = data.get(position);

        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_USER_ID) {
            ((UserIdViewHolder) holder).bind((UserIdInfo) info);
        } else if (viewType == VIEW_TYPE_LINKED_ID) {
            ((LinkedIdViewHolder) holder).bind(context, (LinkedIdInfo) info, isSecret);
        } else {
            throw new IllegalStateException("unhandled identitytype!");
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER_ID) {
            return new UserIdViewHolder(layoutInflater.inflate(R.layout.view_key_identity_user_id, parent, false));
        } else if (viewType == VIEW_TYPE_LINKED_ID) {
            return new LinkedIdViewHolder(layoutInflater.inflate(R.layout.linked_id_item, parent, false));
        } else {
            throw new IllegalStateException("unhandled identitytype!");
        }
    }

    @Override
    public int getItemViewType(int position) {
        IdentityInfo info = data.get(position);
        if (info instanceof UserIdInfo) {
            return VIEW_TYPE_USER_ID;
        } else if (info instanceof LinkedIdInfo) {
            return VIEW_TYPE_LINKED_ID;
        } else {
            throw new IllegalStateException("unhandled identitytype!");
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    public IdentityInfo getInfo(int position) {
        return data.get(position);
    }

    abstract static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class LinkedIdViewHolder extends ViewHolder {
        public final ImageView vVerified;
        final private ImageView vIcon;
        final private TextView vTitle;
        final private TextView vComment;

        public LinkedIdViewHolder(View view) {
            super(view);

            vVerified = (ImageView) view.findViewById(R.id.linked_id_certified_icon);
            vIcon = (ImageView) view.findViewById(R.id.linked_id_type_icon);
            vTitle = (TextView) view.findViewById(R.id.linked_id_title);
            vComment = (TextView) view.findViewById(R.id.linked_id_comment);
        }

        public void bind(Context context, LinkedIdInfo info, boolean isSecret) {
            bindVerified(context, info, isSecret);

            UriAttribute uriAttribute = info.getUriAttribute();
            bind(context, uriAttribute);
        }

        public void bind(Context context, UriAttribute uriAttribute) {
            vTitle.setText(uriAttribute.getDisplayTitle(context));

            String comment = uriAttribute.getDisplayComment(context);
            if (comment != null) {
                vComment.setVisibility(View.VISIBLE);
                vComment.setText(comment);
            } else {
                vComment.setVisibility(View.GONE);
            }

            vIcon.setImageResource(uriAttribute.getDisplayIcon());
        }

        private void bindVerified(Context context, IdentityInfo info, boolean isSecret) {
            if (!isSecret) {
                switch (info.getVerified()) {
                    case Certs.VERIFIED_SECRET:
                        KeyFormattingUtils.setStatusImage(context, vVerified,
                                null, State.VERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                        break;
                    case Certs.VERIFIED_SELF:
                        KeyFormattingUtils.setStatusImage(context, vVerified,
                                null, State.UNVERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                        break;
                    default:
                        KeyFormattingUtils.setStatusImage(context, vVerified,
                                null, State.INVALID, KeyFormattingUtils.DEFAULT_COLOR);
                        break;
                }
            }
        }

        public void seekAttention() {
            if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                ObjectAnimator anim = SubtleAttentionSeeker.tintText(vComment, 1000);
                anim.setStartDelay(200);
                anim.start();
            }
        }
    }

    private static class UserIdViewHolder extends ViewHolder {
        private final TextView vName;
        private final TextView vAddress;
        private final TextView vComment;

        private UserIdViewHolder(View view) {
            super(view);

            vName = (TextView) view.findViewById(R.id.user_id_item_name);
            vAddress = (TextView) view.findViewById(R.id.user_id_item_address);
            vComment = (TextView) view.findViewById(R.id.user_id_item_comment);
        }

        public void bind(UserIdInfo info) {
            if (info.getName() != null) {
                vName.setText(info.getName());
            } else {
                vName.setText(R.string.user_id_no_name);
            }
            if (info.getEmail() != null) {
                vAddress.setText(info.getEmail());
                vAddress.setVisibility(View.VISIBLE);
            } else {
                vAddress.setVisibility(View.GONE);
            }
            if (info.getComment() != null) {
                vComment.setText(info.getComment());
                vComment.setVisibility(View.VISIBLE);
            } else {
                vComment.setVisibility(View.GONE);
            }

            if (info.isPrimary()) {
                vName.setTypeface(null, Typeface.BOLD);
                vAddress.setTypeface(null, Typeface.BOLD);
            } else {
                vName.setTypeface(null, Typeface.NORMAL);
                vAddress.setTypeface(null, Typeface.NORMAL);
            }

        }

    }
}