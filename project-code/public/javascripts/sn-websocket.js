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

                    _this.onmessage(data.result);

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
    $.ajax(this._getUrl() + 'sendmsg/'+msg, {
        type: 'POST',
        success: function(data) {
            console.log("message sent");
        },
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