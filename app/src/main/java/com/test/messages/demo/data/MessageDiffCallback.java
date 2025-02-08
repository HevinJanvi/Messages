package com.test.messages.demo.data;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

public class  MessageDiffCallback extends DiffUtil.ItemCallback<Message> {

    @Override
    public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
        return oldItem.getId() == newItem.getId();
    }

    @Override
    public boolean areContentsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
        return oldItem.getSender().equals(newItem.getSender())
                && oldItem.getContent().equals(newItem.getContent());

    }

    @Override
    public Object getChangePayload(@NonNull Message oldItem, @NonNull Message newItem) {
        return null;
    }
}