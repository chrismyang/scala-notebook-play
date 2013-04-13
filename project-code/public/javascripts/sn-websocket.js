var WebSocket = function (baseUrl) {
    this._baseUrl = baseUrl;

    var _this = this;

    var markAsOpen = function () {
        _this._open = true;
    };

    this._open = false;

    var openConnection = function () {
        $.ajax(baseUrl, {
            type: 'POST',
            success : function (sessionId) {
                _this._sessionId = sessionId;
                markAsOpen();

                startPolling();
            }
        })
    };

    openConnection();


    var isFirstResponse = function () {
        return _this._open;
    };



    var startPolling = function() {
        var poll = function () {
            $.ajax(_this._getUrl() + 'poll', {
                success: function(data) {
                    if (isFirstResponse()) {
                        _this.onopen();
                        markAsOpen();
                    }

                    var messages = data.result;
                    for(var i=0; len=messages.length, i<len; i++) {
                        var message = messages[i];
                        _this.onmessage(message);
                    }

                    if (_this._open) {
                        setTimeout(poll, 2000);
                    }
                },

                error: function(jqXHR, textStatus, errorThrown) {
                    _this._channelFailed(errorThrown);
                }
            });
        }

        poll();
    }
};

WebSocket.prototype._getUrl = function () {
    return this._baseUrl + '/' + this._sessionId + '/';
}

WebSocket.prototype._channelFailed = function (evt) {
    this._open = false;
    this.onclose(evt);
};

WebSocket.prototype.send = function (msg) {
    var _this = this;
    var url = this._getUrl() + 'sendmsg';
    $.ajax({
        url: url,
        type: "POST",
        contentType: "application/json", // send as JSON
        data: msg,
        success: function () {},
        error: function(jqXHR, textStatus, errorThrown) {
        _this._channelFailed(errorThrown);
        }
    });
};

WebSocket.prototype.close = function () {
    this._open = false;
};

WebSocket.prototype.onopen = function () { };

WebSocket.prototype.onclose = function (evt) { };

WebSocket.prototype.onmessage = function (message) { };