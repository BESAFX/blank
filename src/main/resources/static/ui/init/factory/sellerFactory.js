app.factory("SellerService",
    ['$http', '$log', function ($http, $log) {
        return {
            findOne: function (id) {
                return $http.get("/api/seller/findOne/" + id).then(function (response) {
                    return response.data;
                });
            },
            create: function (seller, openCash) {
                return $http.post("/api/seller/create/" + openCash, seller).then(function (response) {
                    return response.data;
                });
            },
            remove: function (id) {
                return $http.delete("/api/seller/delete/" + id);
            },
            update: function (seller) {
                return $http.put("/api/seller/update", seller).then(function (response) {
                    return response.data;
                });
            },
            filter: function (search) {
                return $http.get("/api/seller/filter?" + search).then(function (response) {
                    return response.data;
                });
            }
        };
    }]);