app.factory("ProductPurchaseService",
    ['$http', '$log', function ($http, $log) {
        return {
            findOne: function (id) {
                return $http.get("/api/productPurchase/findOne/" + id).then(function (response) {
                    return response.data;
                });
            },
            findBySeller: function (id) {
                return $http.get("/api/productPurchase/findBySeller/" + id).then(function (response) {
                    return response.data;
                });
            },
            create: function (productPurchase) {
                return $http.post("/api/productPurchase/create", productPurchase).then(function (response) {
                    return response.data;
                });
            },
            remove: function (id) {
                return $http.delete("/api/productPurchase/delete/" + id);
            },
            filter: function (search) {
                return $http.get("/api/productPurchase/filter?" + search).then(function (response) {
                    return response.data;
                });
            }
        };
    }]);