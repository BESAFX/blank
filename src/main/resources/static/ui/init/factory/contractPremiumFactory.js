app.factory("ContractPremiumService",
    ['$http', '$log', function ($http, $log) {
        return {
            findOne: function (id) {
                return $http.get("/api/contractPremium/findOne/" + id).then(function (response) {
                    return response.data;
                });
            },
            findByContract: function (id) {
                return $http.get("/api/contractPremium/findByContract/" + id).then(function (response) {
                    return response.data;
                });
            },
            create: function (contractPremium) {
                return $http.post("/api/contractPremium/create", contractPremium).then(function (response) {
                    return response.data;
                });
            },
            remove: function (id) {
                return $http.delete("/api/contractPremium/delete/" + id);
            },
            sendMessage: function (message, ids) {
                return $http.post("/api/contractPremium/sendMessage/" + ids, message).then(function (response) {
                    return response.data;
                });
            },
            filter: function (search) {
                return $http.get("/api/contractPremium/filter?" + search).then(function (response) {
                    return response.data;
                });
            }
        };
    }]);