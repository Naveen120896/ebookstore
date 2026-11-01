export interface User { id: number; email: string; fullName: string; giftPoints: number; }
export interface AuthResponse { token: string; user: User; }
export interface Category { id: number; name: string; }
export interface Brand { id: number; name: string; }
export interface Product {
  id: number; title: string; author: string; description: string; price: number; stock: number;
  imageUrl: string; category: Category; brand: Brand | null; tentativeDeliveryDate: string;
}
export interface ProductPage { content: Product[]; page: number; size: number; totalElements: number; }
export interface CartItem { product: Product; quantity: number; lineTotal: number; }
export interface Cart { items: CartItem[]; subtotal: number; }
export interface Address {
  id?: number; line1: string; line2?: string; city: string; state?: string;
  postalCode: string; country: string; isDefault?: boolean;
}
export interface OrderItem { productId: number; title: string; quantity: number; unitPrice: number; expectedDelivery: string; }
export interface Order {
  id: number; status: string; items: OrderItem[]; subtotal: number; giftPointsUsed: number;
  total: number; createdAt: string; cancellableUntil: string;
}
export interface PaymentRequest { method: string; cardNumber?: string; cardHolder?: string; expiry?: string; cvv?: string; }
export interface Payment { id: number; orderId: number; method: string; amount: number; status: string; message: string; }
