﻿using Newtonsoft.Json.Linq;
using Serving;
using System;

namespace ChatClient.Protocol
{
    class ChatMessage : JsonMessageHandler<ConnectionToChatServerHandler>
    {
        protected override void Handle(ConnectionToChatServerHandler connection, JObject data)
        {
            var from = data["from"].Value<String>();
            var msg = data["msg"].Value<String>();
            connection.Chat.Global(from, msg);
        }
    }
}
