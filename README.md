# Courier Microservices 🚚

Bu monorepo kargo, kurye ve teslimat süreçleri için tasarlanmış; modern, esnek ve modüler bir Go&Java mikroservis ekosistemidir.

Proje, birbirine karışmayan yalıtılmış paketler halinde tasarlanmış olup **Go Workspaces (`go.work`)** sayesinde kolay bir geliştirme ve test ortamı sunmaktadır.

## 🏗️ Proje Mimarisi (Servisler)

| Mikroservis | Durum | Açıklama |
|---|---|---|
| [Auth Service](./auth-service/) | ✅ Çalışır Durumda | Ekosistemin kapıcı servisi. Kayıt, giriş ve katı JWT Token rotasyonu sağlar. |

> Geliştirilecek diğer modüller (örneğin kargo rotalama, bildirim vb.) buraya eklenecektir.

## 🚀 Başlangıç

Bu ana depo, tüm sistemi bir arada tutar. İlgilendiğiniz herhangi bir servise girerek bağımsız olarak çalıştırabilir ve test edebilirsiniz. Her servisin kurulum, `.env` gereksinimleri ve test talimatları kendi dizinindeki `README.md` dosyasında belgelenmiştir.

Örneğin, `auth-service` ile başlamak için:
```bash
cd auth-service/
cat README.md
```
